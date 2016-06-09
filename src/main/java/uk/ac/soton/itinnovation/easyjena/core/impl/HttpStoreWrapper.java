/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2014
//
// Copyright in this software belongs to University of Southampton
// IT Innovation Centre of Gamma House, Enterprise Road,
// Chilworth Science Park, Southampton, SO16 7NS, UK.
//
// This software may not be used, sold, licensed, transferred, copied
// or reproduced in whole or in part in any manner or form or in or
// on any media by any person other than in accordance with the terms
// of the Licence Agreement supplied with the software, or otherwise
// without the prior written consent of the copyright owners.
//
// This software is distributed WITHOUT ANY WARRANTY, without even the
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE, except where stated in the Licence Agreement supplied with
// the software.
//
//      Created By :            Stefanie Wiegand
//      Created Date :          2014-12-12
//      Created for Project :   REVEAL
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.easyjena.core.impl;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.sparql.resultset.ResultSetException;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.util.JenaUtil;
import uk.ac.soton.itinnovation.easyjena.core.impl.Triple.TripleType;
import uk.ac.soton.itinnovation.easyjena.core.spec.AStoreWrapper;

/**
 * This class provides an interface to a triple store's SPARQL HTTP endpoint.
 */
public class HttpStoreWrapper extends AStoreWrapper {

	private static final Logger logger = LoggerFactory.getLogger(HttpStoreWrapper.class);

	//this is needed to generate the PREFIX statements at the beginning of a SPARQL statement for convenience
	private SemanticFactory semFac;

	//a HTTP SPARQL endpoint might have different GET/POST variables for the actual SPARQL statement submitted
	//depending on whether it is a query (read-only) or an update (read/write)
	private String selectPostVar = "query";
	private String updatePostvar = "update";

	//a HTTP SPARQL endpoint might have different endpoints to execute different actions.
	private String sparqlSelectEndpoint;
	private String sparqlUpdateEndpoint;
	private String sparqlClearEndpoint;

	/**
	 * Creates a HTTP store wrapper to access a triple store via its SPARQL endpoint.
	 *
	 * @param props the required properties containing the endpoint's URL(s)
	 */
	public HttpStoreWrapper(Properties props) {
		super();

		semFac = new SemanticFactory("", "");

		//sanity check of the given properties
		this.props = props;
		if (!props.containsKey("easyjena.sparqlendpoint.select")) {
			throw new RuntimeException("Could not create HttpStoreWrapper, please specify the easyjena.sparqlendpoint"
					+ " property in the properties file.");
		} else {
			sparqlSelectEndpoint = props.getProperty("easyjena.sparqlendpoint.select");
		}
		//optional, i.e. can be null
		if (props.containsKey("easyjena.sparqlendpoint.update")) {
			sparqlUpdateEndpoint = props.getProperty("easyjena.sparqlendpoint.update");
		} else {
			sparqlUpdateEndpoint = sparqlSelectEndpoint;
		}
		if (props.containsKey("easyjena.sparqlendpoint.clear")) {
			sparqlClearEndpoint = props.getProperty("easyjena.sparqlendpoint.clear");
		} else {
			sparqlClearEndpoint = sparqlUpdateEndpoint;
		}
		//auth
		if (props.contains("easyjena.sparqlendpoint.user") || props.containsKey("easyjena.sparqlendpoint.password")) {
			//TODO: implement
//			HttpAuthenticator auth = new HttpAuthenticator();
		}
	}

	// Graph management ///////////////////////////////////////////////////////////////////////////
	@Override
	public boolean graphExists(String graphURI) {
		logger.debug("Checking for the existence of graph {} on server {}", graphURI, sparqlSelectEndpoint);
		//stores are not required to record existence of empty graphs
		//see http://www.w3.org/TR/sparql11-update/#graphManagement
		Query query = QueryFactory.create("ASK { GRAPH <" + graphURI + "> {?s ?p ?o} }");
		QueryEngineHTTP httpQuery = new QueryEngineHTTP(sparqlSelectEndpoint, query);

		//assuming true because the notion of empty graphs is optional, see above
		boolean result = true;
		try {
			//problem in strabon: small ask query is executed as GET but store requires POST
			result = httpQuery.execAsk();
		} catch (Exception e) {
			logger.warn("Could not check for graph existence: {}. Assuming it exists but might be empty.");
		}

		return result;
	}

	@Override
	public void createGraph(String graphURI) {
		//stores are not required to record existence of empty graphs
		//see http://www.w3.org/TR/sparql11-update/#graphManagement
		//this will create an empty graph for stores who do and have no effect otherwise.
		logger.debug("Creating graph <{}>", graphURI);
		update("CREATE GRAPH <" + graphURI + ">");
	}

	@Override
	public void clearGraph(String graphURI) {
		//stores are not required to record existence of empty graphs
		//see http://www.w3.org/TR/sparql11-update/#graphManagement
		//This means it is the same as deleting a graph for some stores.
		logger.debug("Clearing graph <{}>", graphURI);
		update("CLEAR GRAPH <" + graphURI + ">");

		//check if repo is empty and delete all triples if not
		if (getCount(graphURI) > 0) {
			logger.warn("Clearing the graph failed, deleting all triples instead");
			//for stores that don't support named graphs...
			update("DELETE WHERE {?s ?p ?o}");
			//...and for those that do
			update("DELETE {?s ?p ?o} WHERE {GRAPH <" + graphURI + "> {?s ?p ?o}}");
		}
	}

	@Override
	public void deleteGraph(String graphURI) {
		//stores are not required to record existence of empty graphs
		//see http://www.w3.org/TR/sparql11-update/#graphManagement
		logger.debug("Deleting graph <{}>", graphURI);
		update("DROP GRAPH <" + graphURI + ">");

		if (getCount(graphURI) > 0) {
			logger.warn("Deleting graph {} failed, clearing graph instead", graphURI);
			clearGraph(graphURI);
		}
	}

	@Override
	public int getCount(String graphURI) {

		//Different store implementations might support one way or another of determining the amount of triples
		//These two variables collect all information with only the reasonable result being used eventually
		List<Map<String, String>> result1;
		List<Map<String, String>> result2 = null;

		//for stores that don't support named graphs...
		result1 = querySelect("SELECT (COUNT(*) as ?count) WHERE {?s ?p ?o}");
		if (graphURI != null) {
			//...and for those that do
			result2 = querySelect("SELECT (COUNT(*) as ?count) WHERE {GRAPH <" + graphURI + "> {?s ?p ?o}}");
		}

		//evaluate the results
		final String count = "count";
		int count1 = 0;
		if (result1 != null && !result1.isEmpty() && result1.get(0).containsKey(count)) {
			String s = result1.get(0).get(count);
			s = s.substring(1, s.indexOf("^^"));
			if (!s.isEmpty()) {
				count1 = Integer.valueOf(s);
			}
		}
		int count2 = 0;
		if (result2 != null && !result2.isEmpty() && result2.get(0).containsKey(count)) {
			String s = result2.get(0).get(count);
			s = s.substring(1, s.indexOf("^^"));
			if (!s.isEmpty()) {
				count2 = Integer.valueOf(s);
			}
		}

		//use the bigger number since it might be possible that one of the queries wrongly returns 0
		return Integer.max(count1, count2);
	}

	// General actions ////////////////////////////////////////////////////////////////////////////
	@Override
	public void connect() {
		//not needed; this method is only overridden here because other store connectors require this
		logger.debug("Explicitly connecting to a store is unnecessary when using a SPARQL endpoint");
	}

	@Override
	public void disconnect() {
		//not needed; this method is only overridden here because other store connectors require this
		logger.debug("Explicitly dicconnecting from a store is unnecessary when using a SPARQL endpoint");
	}

	@Override
	public void loadNamespaces() {
		//TODO: implement
		throw new java.lang.UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public List<Map<String, String>> querySelect(String sparql) {

		List<Map<String, String>> results = new LinkedList<>();
		ResultSet rs = null;
		try {
			//set the POST parameters:
			Map<String, String> params = new HashMap<>();
			//only accept SPARQL results
			params.put("Accept", "application/sparql-results+xml");
			params.put("Content-Type", "application/sparql-query");
			String response = doPOST(sparqlSelectEndpoint, selectPostVar + "="
					+ URLEncoder.encode(sparql, "UTF-8"), params);

			if (response != null) {
				logger.debug("RESPONSE: {}", response);
				//deserialise XML
				rs = ResultSetFactory.fromXML(response);
			}
		} catch (ResultSetException e) {
			//Empty result retrieved from endpoint: create empty result set from new empty model
			rs = ResultSetFactory.makeResults(JenaUtil.createDefaultModel());
			logger.debug("Exception encountered during HTTP SPARQL query", e);
		} catch (Exception e) {
			logger.error("Could not HTTP encode SPARQL query", sparqlSelectEndpoint, e);
			//not throwing here; failes query will be logged and return an empty result set
		}

		//prepare hashmap
		if (rs != null) {
			while (rs.hasNext()) {
				//for each solution create a hashmap
				QuerySolution row = rs.next();
				Map<String, String> r = new HashMap<>();
				//add all the variables found in the solution
				for (String var : rs.getResultVars()) {
					r.put(var, row.get(var).toString());
				}
				results.add(r);
			}
		}

		return results;
	}

	@Override
	public Model queryConstruct(String sparql) {

		Query query = QueryFactory.create(sparql);
		QueryEngineHTTP httpQuery = new QueryEngineHTTP(sparqlUpdateEndpoint, query);

		Model result = null;
		if (query.getQueryType() == 222) {
			logger.debug("Executing CONSTRUCT query");
			result = httpQuery.execConstruct();
		} else {
			logger.debug("Invalid CONSTRUCT query:\n{}", sparql);
		}

		return result;
	}

	@Override
	public Model queryDescribe(String sparql) {

		Model m = null;

		Query query = QueryFactory.create(sparql);
		//execute DESCRIBE query on read-only endpoint
		QueryEngineHTTP httpQuery = new QueryEngineHTTP(sparqlSelectEndpoint, query);

		if (query.getQueryType() == 222) {
			logger.debug("Executing DESCRIBE query");
			m = httpQuery.execConstruct();
		} else {
			logger.error("Invalid DESCRIBE query:\n{}", sparql);
			//don't throw here; only return empty result set and log error
		}
		return m;
	}

	@Override
	public boolean queryAsk(String sparql) {

		boolean result = false;

		Query query = QueryFactory.create(sparql);
		//execute ASK query on read-only endpoint
		QueryEngineHTTP httpQuery = new QueryEngineHTTP(sparqlSelectEndpoint, query);

		if (query.getQueryType() == 444) {
			logger.debug("Executing ASK query");
			result = httpQuery.execAsk();
		} else {
			logger.error("Invalid ASK query:\n{}", sparql);
			//don't throw here; return "false" log error
		}
		return result;
	}

	@Override
	public void update(String sparql) {
		//logger.info("Updating endpoint {} with query {}", sparqlUpdateEndpoint, sparql);
		try {
			Map<String, String> params = new HashMap<>();
			//accept server response only as rdf/xml. More options might be implemented in the future
			params.put("Accept", "application/rdf+xml");
			params.put("Content-Type", "application/sparql-query");
			doPOST(sparqlUpdateEndpoint, updatePostvar + "=" + URLEncoder.encode(sparql, "UTF-8"), params);
		} catch (UnsupportedEncodingException e) {
			//don't throw here, only notify user
			logger.error("Could not HTTP encode SPARQL update {} for execution on endpoint {}",
					sparql, sparqlUpdateEndpoint, e);
		}
	}

	// Actions that might be executed on a particular graph ///////////////////////////////////////
	@Override
	public void importDocumentToGraph(String ontologypath, String baseURI, String graphURI, RDFFormat format) {
		//TODO: implement
		throw new java.lang.UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getRDF(String graphURI) {
		//TODO: implement
		throw new java.lang.UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void storeTriple(Triple t, String graphURI) {
		logger.debug("Storing triple {}", t.toString());
		String obj;
		//for all data properties...
		if (t.getType().equals(TripleType.DATA_PROPERTY)) {
			//attach quotes in case they don't exist and remove newlines
			if (!t.getObject().startsWith("\"")) {
				obj = "\"" + t.getObject().replaceAll("\"", "'").replaceAll("\n", " ") + "\"";
			} else {
				obj = t.getObject().replaceAll("\"", "'").replaceAll("\n", " ");
			}
		} else {
			//TODO: annotation props
			obj = "<" + t.getObject() + ">";
		}

		//build SPARQL insert with or without graph
		String sparql = sparqlPrefixes + "\nINSERT DATA {\n";
		if (graphURI != null) {
			sparql += "\tGRAPH <" + graphURI + "> {\n";
		}
		sparql += "\t<" + t.getSubject() + "> <" + t.getPredicate() + "> " + obj + "\n";
		if (graphURI != null) {
			sparql += "\t}\n";
		}
		sparql += "}\n";

		//execute
		update(sparql);
	}

	@Override
	public void removeTriple(Triple t, String graphURI) {
		//TODO: implement
		throw new java.lang.UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void storeTriples(Set<Triple> triples, String graphURI) {
		//TODO: implement
		throw new java.lang.UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void removeTriples(Set<Triple> triples, String graphURI) {
		//TODO: implement
		throw new java.lang.UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void storeModel(Model m, String graphURI) {



		//get all triples from the given model
		StmtIterator it = m.listStatements();
		StringBuilder sparql = new StringBuilder("INSERT DATA {\n");
		if (graphURI != null) {
			sparql.append(String.join("\tGRAPH <", graphURI, "> {\n"));
		}

		//build giant insert query
		//TODO: split from certain size onwards?
		while (it.hasNext()) {
			Statement stmt = it.next();
			String obj;
			String sparqlObj;
			TripleType type;
			//TODO: annotation properties?!
			//wrap URIs in angle brackets
			if (stmt.getObject().isURIResource()) {
				obj = stmt.getObject().toString();
				sparqlObj = "<" + obj + ">";
				type = TripleType.OBJECT_PROPERTY;
			} else if (stmt.getObject().isLiteral()) {
				obj = stmt.getObject().asLiteral().getString();
				String xsdtype = stmt.getObject().asLiteral().getDatatypeURI();
				//remove quotes if they exist
				if (obj.startsWith("\"") && obj.endsWith("\"")) {
					sparqlObj = obj.substring(1, obj.length() - 1);
				} else {
					sparqlObj = obj;
				}
				//add triple quotes and datatype if applicable
				final String tripleQuotes = "\"\"\"";
				if (xsdtype != null) {
					sparqlObj = tripleQuotes + sparqlObj + tripleQuotes + "^^" + semFac.toShortURI(xsdtype);
				} else {
					sparqlObj = tripleQuotes + sparqlObj + tripleQuotes;
				}
				type = TripleType.DATA_PROPERTY;
			} else if (stmt.getObject().isAnon()) {
				obj = stmt.getObject().toString();
				sparqlObj = obj;
				type = TripleType.OBJECT_PROPERTY;
			} else {
				obj = stmt.getObject().toString();
				sparqlObj = obj;
				type = TripleType.UNKNOWN;
			}
			//build triple
			Triple t = new Triple(stmt.getSubject().getURI(), stmt.getPredicate().getURI(), obj, type);
			sparql.append(String.join("\t\t<", t.getSubject(), "> <", t.getPredicate(), "> ", sparqlObj, " .\n"));
		}
		if (graphURI != null) {
			sparql.append("\t}\n");
		}
		sparql.append("}");

		//execute
		update(sparql.toString());
	}

	@Override
	public void removeModel(Model m, String graphURI) {
		//TODO: implement
		throw new java.lang.UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void storeRDF(String rdf, String graphURI) {
		//TODO: implement
		throw new java.lang.UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void removeRDF(String rdf, String graphURI) {
		//TODO: implement
		throw new java.lang.UnsupportedOperationException("Not supported yet.");
	}

	// Private Methods ////////////////////////////////////////////////////////////////////////////
	/**
	 * Execute a HTTP POST request on the store
	 *
	 * @param endpoint the SPARQL HTTP endpoint
	 * @param payload the actual payload of the query
	 * @param parameters the HTTP parameters
	 * @return the result retrieved back from the endpoint
	 */
	private String doPOST(String endpoint, String payload, Map<String, String> parameters) {

		String r = null;

		try {
			//open connection to the endpoint
			URL obj = new URL(endpoint);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			//set request properties
			con.setRequestMethod("POST");
			if (parameters != null) {
				parameters.entrySet().stream().forEach(param
						-> con.setRequestProperty(param.getKey(), param.getValue())
				);
			}

			// Send post request
			con.setDoOutput(true);
			try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
				wr.writeBytes(payload);
				wr.flush();
			}

			//get response
			int responseCode = con.getResponseCode();
			logger.debug("Response code for POST query on endpoint {}: {}", endpoint, responseCode);
			StringBuilder response;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				response = new StringBuilder();
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
			}
			r = response.toString();

		} catch (Exception e) {
			logger.error("Could not send HTTP request to URL {}", endpoint, e);
		}
		return r;
	}

	// Getters/Setters ////////////////////////////////////////////////////////////////////////////
	public String getSelectPostVar() {
		return selectPostVar;
	}

	public void setSelectPostVar(String selectPostVar) {
		this.selectPostVar = selectPostVar;
	}

	public String getUpdatePostvar() {
		return updatePostvar;
	}

	public void setUpdatePostvar(String updatePostvar) {
		this.updatePostvar = updatePostvar;
	}

	public String getSparqlSelectEndpoint() {
		return sparqlSelectEndpoint;
	}

	public void setSparqlSelectEndpoint(String sparqlSelectEndpoint) {
		this.sparqlSelectEndpoint = sparqlSelectEndpoint;
	}

	public String getSparqlUpdateEndpoint() {
		return sparqlUpdateEndpoint;
	}

	public void setSparqlUpdateEndpoint(String sparqlUpdateEndpoint) {
		this.sparqlUpdateEndpoint = sparqlUpdateEndpoint;
	}

	public String getSparqlClearEndpoint() {
		return sparqlClearEndpoint;
	}

	public void setSparqlClearEndpoint(String sparqlClearEndpoint) {
		this.sparqlClearEndpoint = sparqlClearEndpoint;
	}

}
