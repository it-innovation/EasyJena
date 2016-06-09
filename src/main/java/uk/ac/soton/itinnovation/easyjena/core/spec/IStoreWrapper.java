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
//		Created By :				Stefanie Wiegand
//		Created Date :			2014-04-07
//		Created for Project:		OPTET
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.easyjena.core.spec;

import com.hp.hpl.jena.rdf.model.Model;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.jena.riot.RDFFormat;
import uk.ac.soton.itinnovation.easyjena.core.impl.Triple;


/**
 * This interface specifies a triple store, in which ontology models can be saved for further processing.
 * It can either be a using native libraries or SPARQL endpoints for accessing the store.
 */
public interface IStoreWrapper {

	// Graph management ///////////////////////////////////////////////////////////////////////////

	/**
	 * Checks for the existence of a given graph
	 *
	 * @param graphURI the ID of the graph to check for
	 * @return whether the graph exists
	 */
	boolean graphExists(String graphURI);

	/**
	 * Create a new graph. The argument passed is the id or name of the graph and it is
	 * up to the implementing method to decide what to do with it.
	 *
	 * @param graphURI the id or name of the graph
	 */
	void createGraph(String graphURI);

	/**
	 * Clears the store which means it is reset to its original state, containing no triples or other content.
	 *
	 * @param graphURI the graph to be cleared
	 */
	void clearGraph(String graphURI);

	/**
	 * Deletes the given graph if it exists
	 *
	 * @param graphURI
	 */
	void deleteGraph(String graphURI);

	/**
	 * Return the amount of triples contained in the store
	 *
	 * @param graphURI the graph for which to count
	 * @return the amount of triples
	 */
	int getCount(String graphURI);

	// General actions ////////////////////////////////////////////////////////////////////////////

	/**
	 * Start a connection to the store.
	 * This does not require a specific graph to be selected yet, but a store implementation
	 * may choose to preselect a default graph.
	 */
	void connect();

	/**
	 * End the connection to the store. Release all resources.
	 */
	void disconnect();

	/**
	 * Loads all prefixes used in the store and saves them to the prefixes string and namespace map.
	 * This makes it unnecessary for the user to specify all prefixes manually in each SPARQL query.
	 */
	void loadNamespaces();

	/**
	 * Queries the store and returns the results in a persistent form (unlike the QueryResult object
	 * which is emptied upon reading/printing)
	 *
	 * @param sparql the SPARQL SELECT query (doesn't need prefix statements if previously specified)
	 * @return the results of the query, in whatever form the implementing class chooses
	 */
	Object querySelect(String sparql);

	/**
	 * Queries the store
	 *
	 * @param sparql the SPARQL CONSTRUCT query (doesn't need prefix statements if previously specified)
	 * @return the constructed triples in whatever form the implementing class chooses
	 */
	Object queryConstruct(String sparql);

	/**
	 * Queries the store and returns the resulting triples as a model
	 *
	 * @param sparql the SPARQL DESCRIBE query (doesn't need prefix statements if previously specified)
	 * @return the results of the query in whatever form the implementing class chooses
	 */
	Object queryDescribe(String sparql);

	/**
	 * Queries the store and returns the result
	 *
	 * @param sparql the SPARQL ASK query (doesn't need prefix statements if previously specified)
	 * @return the results of the query
	 */
	boolean queryAsk(String sparql);

	/**
	 * Runs an update (e.g. INSERT, DELETE, ...) on a SPARQL update compatible endpoint
	 * @param sparql the update query to run
	 */
	void update(String sparql);

	/**
	 * Translate the result of a select query
	 *
	 * @param results the results object as returned by querySelect(...)
	 * @return a list containing the results where each element is a map of all variables
	 */
	List<Map<String, String>> translateSelectResult(Object results);

	// Actions that might be executed on a particular graph ///////////////////////////////////////

	/**
	 * Imports an ontology from a document into the store.
	 *
	 * @param ontologypath where to find the ontology document. This can be a URL or a path on disk.
	 * @param baseURI the baseURI of the ontology - none if it is null
	 * @param graphURI which graph to save it into - default graph is this is null
	 * @param format the format the document is in
	 */
	void importDocumentToGraph(String ontologypath, String baseURI, String graphURI, RDFFormat format);

	/**
	 * Get the RDF representation of the contents of the repository
	 *
	 * @param graphURI the URI of the graph; default graph if this is null
	 * @return the RDF string
	 */
	String getRDF(String graphURI);

	/**
	 * Stores a single triple in the store. Since this method should be a transaction,
	 * it is discouraged from using it for large amounts of triples.
	 *
	 * @param t the triple to be stored
	 * @param graphURI the URI of the graph; default graph if this is null
	 */
	void storeTriple(Triple t, String graphURI);

	/**
	 * Remove a specific triple from the store
	 *
	 * @param t the triple to remove
	 * @param graphURI the URI of the graph; default graph if this is null
	 */
	void removeTriple(Triple t, String graphURI);

	/**
	 * Stores a set of triples. This should be implemented to be one transaction for n triples stored.
	 *
	 * @param triples the triples to store
	 * @param graphURI the URI of the graph; default graph if this is null
	 */
	void storeTriples(Set<Triple> triples, String graphURI);

	/**
	 * Removes a set of triples from the store
	 *
	 * @param triples the triples to remove
	 * @param graphURI the URI of the graph; default graph if this is null
	 */
	void removeTriples(Set<Triple> triples, String graphURI);

	/**
	 * Stores a model in the store
	 *
	 * @param m the model to store
	 * @param graphURI the URI of the graph; default graph if this is null
	 */
	void storeModel(Model m, String graphURI);

	/**
	 * Removes a model from the store
	 *
	 * @param m the model to remove
	 * @param graphURI the URI of the graph; default graph if this is null
	 */
	void removeModel(Model m, String graphURI);

	/**
	 * Stores a serialised RDF string in the store.
	 *
	 * @param rdf the rdf to store
	 * @param graphURI the URI of the graph; default graph if this is null
	 */
	void storeRDF(String rdf, String graphURI);

	/**
	 * Removes serialised RDF from the store
	 *
	 * @param rdf the RDF to remove
	 * @param graphURI the URI of the graph; default graph if this is null
	 */
	void removeRDF(String rdf, String graphURI);

	// Simple Getters /////////////////////////////////////////////////////////////////////////////

	/**
	 * Get a String containing of all the prefixes used in the store for querying as they would
	 * appear at the beginning of each SPARQL query
	 *
	 * @return the prefixes
	 */
	String getSPARQLPrefixes();

	/**
	 * Find out if there is an active connection to the store at this instant.
	 *
	 * @return whether it is connected
	 */
	boolean isConnected();

	/**
	 * Get the namespace mappings used in the store
	 *
	 * @return the namespace mapping
	 */
	Map<String, String> getPrefixURIMap();

	/**
	 * Get the properties object of this store.
	 * May be null if the implementation does not use properties.
	 *
	 * @return the properties
	 */
	Properties getProperties();

	/**
	 * Find out whether this store supports GEOSPARQL
	 * @return true if it does, false otherwise
	 */
	boolean hasGeoSupport();

}
