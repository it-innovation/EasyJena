/////////////////////////////////////////////////////////////////////////
//
// (c) University of Southampton IT Innovation Centre, 2014
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
package uk.ac.soton.itinnovation.easyjena.core.impl;

import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.ResourceRequiredException;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.shared.PrefixMapping.IllegalPrefixException;
import com.hp.hpl.jena.sparql.resultset.ResultSetMem;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.constraints.ConstraintViolation;
import org.topbraid.spin.constraints.SPINConstraints;
import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.model.Argument;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.JenaUtil;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.soton.itinnovation.easyjena.core.impl.Triple.TripleType;
import uk.ac.soton.itinnovation.easyjena.core.spec.IStoreWrapper;

/**
 * General ontology manipulation utilities. While this class executes operations like
 * loading/saving, adding/removing information, reasoning, querying etc. on in-memory Jena models,
 * it does not keep a copy of the model itself; this is the responsibility of the developer to
 * maintain their own model(s).
 *
 * However, a list of prefixes, URIs and import locations from disk is maintained for the ontologies
 * handled using this class. This configuration can be imported/exported at any time.
 */
public class JenaOntologyManager {

	private static final Logger logger = LoggerFactory.getLogger(JenaOntologyManager.class);

	//constant for the source location option
	private static final String SOURCE_PATH = "easyjena.sourcePath";
	//constant for owl:imports URI
	private static final String OWL_IMPORTS = "http://www.w3.org/2002/07/owl#imports";
	//max number of spin classification re-runs before stopping to prevent an endless loop
	private static final int MAX_SPIN_ITERATIONS = 100;
	//for properties documentation see src/main/resources/easyjena.properties
	protected Properties props;
	//mainly for internal use. don't rely on it to maintain a map consistently!
	protected SemanticFactory semanticFactory;
	//a temporary model for incremental reasoning
	protected Model tmpModel;
	//keeps track of all the import mappings where the ontology type differs from the URI
	protected Map<String, ImportMapping> importLocationMap;

	/**
	 * Creates an "empty" JenaOntologyManager.
	 */
	public JenaOntologyManager() {

		clear();
		props = new Properties();
		props.setProperty(SOURCE_PATH, "");
	}

	/**
	 * Creates a JenaOntologyManager using given properties
	 *
	 * @param props the properties object. For more information on what properties can be used
	 * please view the easyjena.properties file.
	 */
	public JenaOntologyManager(Properties props) {

		this();
		setProps(props);
	}

	// Housekeeping ///////////////////////////////////////////////////////////////////////////////
	/**
	 * Clear all contents of this manager, empty all models and return to a fresh state.
	 * Retains all properties (they can be replaced separately)
	 */
	public final void clear() {

		tmpModel = JenaUtil.createDefaultModel();
		importLocationMap = new HashMap<>();
		semanticFactory = new SemanticFactory();
	}

	/**
	 * Adds a mapping to an import type to the internal map in case the URI
 used does not really exist. You can specify
	- an absolute location on disk (e.g. "/location/to/ontologies/myontology.rdf").
	- a relative location on disk (using the sourcePath property), including filename only (e.g. "foaf.rdf").
	- a relative location on the classpath, including filename only, (e.g. "ontologies/foaf.rdf").
	- an alternative URL

 This method needs to be executed prior to loading an ontology and makes sure the mapping is correct.
 If a combination of loading locations is used, the priority is:
		disk > web > classpath
	 *
	 * @param uri the ontology's URI/namespace
	 * @param location the type from where to load the ontology rather than from its URI
	 * @param type type of this import mapping, @see JenaOntologyManager.LoadingLocation
	 */
	public void addImportLocationMapping(String uri, String location, LoadingLocation type) {

		String absolutePath = location;

		//check if type exists:
		boolean found = false;

		//local file
		if (type.getNumVal() >= LoadingLocation.DIRECTORY.getNumVal()) {
			//test if file exists
			absolutePath = location;
			File file = new File(absolutePath);
			if (file.isFile()) {
				found = true;
				logger.debug("Absolute local file {} found for URI <{}>", absolutePath, uri);
			} else {
				//if it doesn't test for relative location
				absolutePath = props.getProperty(SOURCE_PATH) + location;
				file = new File(absolutePath);
				if (file.isFile()) {
					found = true;
					logger.debug("Relative local file {} found in directory {} for URI <{}>",
							location, props.getProperty(SOURCE_PATH), uri);
				}
			}
		}

		//web
		if (!found && (type.getNumVal()%2)!=0) {
			found = urlExists(location);
			if (found) {
				absolutePath = location;
				logger.debug("Alternative URI <{}> specified for ontology <{}>", location, uri);
			}
		}

		//classpath
		if (!found && (type.equals(LoadingLocation.CLASSPATH) || type.equals(LoadingLocation.WEB_AND_CLASSPATH)
				|| type.equals(LoadingLocation.CLASSPATH_AND_DIRECTORY) || type.equals(LoadingLocation.ALL))) {

			//TODO: what if the calling method is a class that extends JOM?
			URL resource = this.getClass().getClassLoader().getResource(location);
			if (resource != null && resource.getPath() != null) {
				File file = new File(resource.getPath());
				absolutePath = file.getAbsolutePath();
				if (file.isFile()) {
					found = true;
					logger.debug("Relative file {} on classpath found for URI <{}>", location, uri);
				}
			}
		}

		//the mapping could be resolved to an existing type
		if (found) {
			//check if type has already been specified; overwrite with new mapping if it exists
			if (importLocationMap.containsKey(uri)) {
				logger.info("There was already an import location defined for URI <{}>. New location: {}", uri, absolutePath);
			} else {
				logger.info("Adding mapping for URI <{}>. Location: {}", uri, absolutePath);
			}
			//add the mapping
			importLocationMap.put(uri, new ImportMapping(uri, absolutePath, type));
		} else {
			logger.warn("The mapping location {} you provided for URI <{}> could not be found at {}", location, uri,
					type.toString());
		}
	}

	/**
	 * Removes the mapping to an import type for the given URI
	 *
	 * @param uri the uri of the ontology
	 */
	public void removeImportLocationMapping(String uri) {

		if (uri!=null) {
			//check all mappings
			Iterator<Map.Entry<String, ImportMapping>> it = importLocationMap.entrySet().iterator();
			while (it.hasNext()) {
				ImportMapping im = it.next().getValue();
				//if it relates to the given URI...
				if (im.getUri().equals(uri)) {
					//...remove it
					it.remove();
				}
			}
		}
	}

	/**
	 * Load an XML file containing alternative import locations for ontologies, e.g.:
	 *
	 *	<?xml version="1.0" encoding="UTF-8"?>
	 *	<configs>
	 *
	 *			<file>
	 *				<uri>http://www.w3.org/2001/XMLSchema#</uri>
	 *				<path>ontologies/XMLSchema.xsd</path>
	 *				<location>[disk|web|classpath]</location>
	 *			</file>
	 *
	 *	</configs>
	 *
	 * @param xmlpath the location of the XML config file
	 */
	public void loadImportLocationConfigFile(String xmlpath) {

		try {
			logger.debug("Loading import location config file {}", xmlpath);
			XMLConfiguration config = new XMLConfiguration(xmlpath);

			// get all mappings
			NodeList files = config.getDocument().getElementsByTagName("file");
			for (int i = 0; i < files.getLength(); i++) {
				Node file = files.item(i);
				NodeList tags = file.getChildNodes();

				String uri = null;
				String location = null;
				//use "all" as default loading type
				LoadingLocation type = LoadingLocation.ALL;

				for (int j = 0; j < tags.getLength(); j++) {
					Node tag = tags.item(j);

					switch (tag.getNodeName()) {
						case "uri":
							uri = tag.getTextContent();
							break;

						case "path":
							location = tag.getTextContent();
							break;

						case "location":
							switch (tag.getTextContent()) {
								case "disk":
									type = LoadingLocation.DIRECTORY;
									break;
								case "web":
									type = LoadingLocation.WEB;
									break;
								case "classpath":
									type = LoadingLocation.CLASSPATH;
									break;
								default:
									logger.warn("Invalid loading location provided: {}. " +
											"Needs to be one of [disk, web, classpath]", tag.getTextContent());
							}
							break;

						default:
						//do nothing
					}
				}

				if (uri != null && location != null) {
					addImportLocationMapping(uri, location, type);
				} else {
					logger.warn("Incomplete entry in XML file, skipping <{}> ({})", uri, location);
				}
			}
		} catch (ConfigurationException e) {
			logger.error("Error loading import locations XML file {}", xmlpath, e);
			//no throwing necessary, will continue without any mapped loactions
		}
	}

	/**
	 * Create a list of HashMaps which represent a Jena ResultSet for easier use
	 *
	 * @param result the result set
	 * @return the reformatted results
	 */
	public List<Map<String, String>> createMap(ResultSet result) {

		List<Map<String, String>> results = new LinkedList<>();
		if (result != null) {
			while (result.hasNext()) {
				QuerySolution row = result.next();
				Map<String, String> r = new HashMap<>();
				for (String var: result.getResultVars()) {
					if (row.contains(var)) {
						r.put(var, row.get(var).toString());
						results.add(r);
					}
				}

			}
		}
		return results;
	}

	/**
	 * Adds a new prefix URI mapping to the given model and internal map of prefixes
	 *
	 * @param prefix the prefix
	 * @param uri the uri
	 * @param model the input model
	 * @return the input model plus the prefix mapping
	 */
	public Model addPrefixURIMapping(String prefix, String uri, Model model) {

		try {
			model.setNsPrefix(prefix, uri);
			semanticFactory.addPrefixURIMapping(prefix, uri);
		} catch (IllegalPrefixException ex) {
			logger.warn("Could not set prefix mapping {}:<{}>", prefix, uri, ex);
		}
		return model;
	}

	/**
	 * Changes the baseURI of this model. This is the same as setting the empty prefix to this URI.
	 * The old namespace will be overwritten.
	 *
	 * @param baseURI the new baseURI to set
	 * @param model the input model
	 * @return the model with its new namespace
	 */
	public Model setBaseURI(String baseURI, Model model) {

		logger.debug("Overwriting old baseURI {} with new baseURI {}", model.getNsPrefixURI(""), baseURI);
		return addPrefixURIMapping("", baseURI, model);
	}

	// Create model ///////////////////////////////////////////////////////////////////////////////
	/**
	 * Creates a new model with the given URI
	 *
	 * @param uri the base URI of the new model
	 * @return the new model
	 */
	public Model createNewModel(String uri) {
		Model m = JenaUtil.createDefaultModel();
		setBaseURI(uri, m);
		return m;
	}

	/**
	 * Loads the imports for the given model
	 * @param m the model
	 * @param location where to load imports from
	 * @return the model with loaded imports
	 */
	public Model loadImports(Model m, LoadingLocation location) {
		return loadOntologyModel(m, m.getNsPrefixURI(""), location);
	}

	// Loading ////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Loads an ontology into memory from a given location.
	 *
	 * @param uri the URI of the ontology to load. If it's not actually in the given loaction,
	 *			a mapping needs to be added *before* calling this method
	 * @param location the default location to load imports from. if "none", no imports will be loaded.
	 *			individual import mappings will override this default setting
	 * @return the loaded model
	 * @throws java.io.FileNotFoundException this is thrown if the file cannot be found at the specified location
	 *
	 * @see JenaOntologyManager.LoadingLocation
	 */
	public Model loadOntology(String uri, LoadingLocation location) throws FileNotFoundException {

		logger.info("Loading ontology from URI");

		Model m = null;
		String baseURI = null;
		try {

			//load the model itself
			m = FileManager.get().loadModel(uri);

			//find baseURI:
			//...either baseURI is defined in the model
			if (m.getNsPrefixMap().containsKey("") && m.getNsPrefixURI("")!=null && !m.getNsPrefixURI("").isEmpty()) {
				baseURI = m.getNsPrefixURI("");
			//...if it isn't we can still look at the uri, which might be the ontology's URL
			} else if (m.getNsPrefixMap().containsValue(uri)) {
				baseURI = uri;
			} else {
				logger.debug("This ontology doesn't have a base URI. Very sad, but there's nothing we can do :(");
			}

		} catch (Exception e) {
			logger.error("Could not load ontology <{}>", uri, e);
			throw new FileNotFoundException("Could not load ontology <" + uri + ">. If it's not actually " +
					"located at this URI, please add a location mapping before attempting to load.");
		}

		return loadOntologyModel(m, baseURI, location);
	}

	/**
	 * Loads an ontology from an InputStream
	 *
	 * @param stream the stream from which the model is read
	 * @param baseURI the baseURI of the model
	 * @param format format of the ontology stream,
	 * @param location the default location to load imports from. if "none", no imports will be loaded.
	 *			individual import mappings will override this default setting
	 * @return the loaded model
	 * @throws java.io.IOException if the ontology could not be read from the given stream
	 *
	 * @see com.hp.hpl.jena.rdf.model.Model.read()
	 * @see JenaOntologyManager.ModelFormat
	 * @see JenaOntologyManager.LoadingLocation
	 */
	public Model loadOntologyFromStream(InputStream stream, String baseURI, ModelFormat format,
			LoadingLocation location) throws IOException {

		Model m = null;

		try {
			logger.info("Loading ontology from InputStream");
			//start with an empty model
			m = ModelFactory.createDefaultModel();
			//deserialise the stream
			m.read(stream, baseURI, format.getText());
		} catch (Exception e) {
			logger.error("Could not load ontology <{}> from stream", baseURI, e);
			throw new IOException("Could not load ontology <" + baseURI + "> from stream. Perhaps " +
					"the stream is corrupt or the baseURI is incorrect.");
		}

		return loadOntologyModel(m, baseURI, location);
	}

	/**
	 * Loads ontology from a serialised string
	 *
	 * @param ontRDFString serialized ontology string
	 * @param baseURI model base URI
	 * @param format format of the ontology stream
	 * @param encoding the string's encoding. uses utf-8 by default (if null)
	 * @param location the loading type for import statements
	 * @return the loaded model
	 * @throws java.io.UnsupportedEncodingException if the encoding is not supported
	 * @throws java.io.IOException if the stream could not be read for any reason
	 *
	 * @see JenaOntologyManager.ModelFormat
	 * @see JenaOntologyManager.LoadingLocation
	 */
	public Model loadOntologyFromString(String ontRDFString, String baseURI, ModelFormat format,
			String encoding, LoadingLocation location) throws IOException {

		logger.info("Loading ontology from serialised string");

		Model m = null;
		InputStream stream = null;
		//use UTF-8 as standard encoding
		String enc = encoding;
		if (enc==null) {
			enc = "UTF-8";
		}

		try {
			//start with an empty model
			m = ModelFactory.createDefaultModel();
			stream = new ByteArrayInputStream(ontRDFString.getBytes(enc));
			m.read(stream, baseURI, format.getText());
			m = loadOntologyModel(m, baseURI, location);

		} catch (UnsupportedEncodingException e) {
			logger.error("Encoding error while reading ontology <{}> from serialised string", baseURI, e);
			throw new UnsupportedEncodingException("Could not load ontology <" + baseURI + "> from serialised string. "
					+ "The encoding was incorrect.");
		} catch (Exception e) {
			logger.error("Could not load ontology <{}> from stream", baseURI, e);
			throw new IOException("Could not load ontology <" + baseURI + "> from serialised string. ");

		} finally {
			//close stream in case it has been opened
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException ex) {
					logger.error("Error closing RDF stream", ex);
					//not throwing here as it would not affect the functionality
				}
			}
		}

		return loadOntologyModel(m, baseURI, location);
	}

	/**
	 * Load an ontology from a triple store
	 *
	 * @param store the store to load from
	 * @param location the loading type for import statements
	 * @return the loaded model
	 *
	 * @see JenaOntologyManager.LoadingLocation
	 */
	public Model loadOntologyFromStore(IStoreWrapper store, LoadingLocation location) {

		if (store == null) {
			logger.error("Invalid store {}, could not load ontology", store);
			return null;
		}

		logger.info("Loading ontology from store using {}", store.getClass().getSimpleName());
		Model m = JenaUtil.createDefaultModel();

		//get namespaces
		store.loadNamespaces();
		logger.debug("prefix map from store: {}", store.getPrefixURIMap().entrySet());

		//TODO: too slow - takes about 4 minutes for ~40k triples
		//String rdf = store.getRDF();
		//logger.debug(rdf);
		//loadOntology(rdf, baseURI, LoadingLocation.NONE);
		List<Map<String, String>> result = store.translateSelectResult(
			store.querySelect("SELECT * WHERE { ?s ?p ?o }")
		);
		if (result == null) {
			logger.error("The result for this SELECT query was null");
			return null;
		}
		Iterator<Map<String, String>> it = result.iterator();
		while (it.hasNext()) {
			Map<String, String> row = it.next();

			//might be URI or blank
			String subject = row.get("s");
			ResourceImpl subj;
			if (subject.startsWith("_")) {
				subj = new ResourceImpl(new AnonId(subject));
			} else {
				subj = new ResourceImpl(subject);
			}
			//always URI
			String predicate = row.get("p");
			//might be URI, blank or literal
			String object = row.get("o");
			RDFNode obj;
			if (object.startsWith("_")) {
				obj = new ResourceImpl(new AnonId(object));
				//TODO: find a better solution
			} else if (object.contains("^^") || (object.trim().startsWith("\"") && object.trim().endsWith("\""))) {
				obj = new LiteralImpl(NodeFactory.createLiteral(object), new ModelCom(m.getGraph()));
			} else {
				obj = new ResourceImpl(object);
			}

			Statement s = new StatementImpl(subj, new PropertyImpl(predicate), obj);
			m.add(s);
		}
		return loadOntologyModel(m, null, location);
	}

	/**
	 * Loads an ontology's imports from a model and initialise the ontology manager for use with this model.
	 * This is not supposed to be used externally and thus declared protected.
	 *
	 * @param m the model to process
	 * @param baseURI the baseURI of this model
	 * @param location the loading type for import statements
	 * @return the loaded ontology
	 *
	 * @see JenaOntologyManager.LoadingLocation
	 */
	protected Model loadOntologyModel(Model m, String baseURI, LoadingLocation location) {

		//sanity check
		LoadingLocation loc = location;
		if (location==null) {
			logger.info("No loading location has been specified. Using all possible locations by default.");
			loc = LoadingLocation.ALL;
		}

		long start = System.currentTimeMillis();

		//this is the starting point for the model to be returned. If imports are loaded, this will be enhanced
		Model loadedModel = m;

		//init semantic factory as a container for prefixes
		semanticFactory.addMappingsFromModel(m);

		//add prefixes from map to jena map (needs to be done to include "standard" prefixes from semantic factory
		m.setNsPrefixes(semanticFactory.getNamespaces());

		//check imports are supposed to be loaded
		if (!loc.equals(LoadingLocation.NONE)) {

			logger.debug("Loading location: {}", loc.toString());

			//collect imports
			Map<String, String> startMap = new HashMap<>();

			//iterate over all imports. key is the short prefix, value is the IRI, then recursively load imports.
			Iterator<Map.Entry<String, String>> it = getImports(m, false).entrySet().iterator();
			while (it.hasNext()) {

				Map.Entry<String, String> s = it.next();
				logger.debug("Next namespace: " + s.toString());

				//prepare variables
				String prefix = s.getKey();
				String iri = s.getValue();

				//collect import in map
				startMap.put(prefix, iri);

				//already add to namespaces map (grab every namespace here we can get for a better factory)
				semanticFactory.addPrefixURIMapping(prefix, iri);

				//skip base ontology and ignore existing mappings
				if (prefix.isEmpty() || importLocationMap.containsKey(iri)
						//TODO: is there a better way to skip ontotext ontologies because of timeout?
						|| ("pext".equals(prefix) && "http://proton.semanticweb.org/protonext#".equals(iri))
						|| ("psys".equals(prefix) && "http://proton.semanticweb.org/protonsys#".equals(iri))) {
					continue;
				}

				//if no mapping exists, use default location setting
				if (!importLocationMap.containsKey(iri)) {

					boolean found = false;

					//the obvious choice: load from web if allowed
					if (loc.getNumVal()%2!=0) {
						logger.debug("Trying to load <{}> from web...", iri);
						//Check URL exists. This is not a duplicate of the checking method in addImportLocationMapping()
						//but a complement. This check is for ontologies, for which no mapping has been added.
						found = urlExists(iri);
					}

					//unless the mapping has been specified prior to this method being called, we have no way of
					//knowing where to load from.
					if (!found) {
						logger.debug("Import <{}> could not be found", iri);
					}
				}
			}

			//recursively add namespaces, starting with the original ontology's imports
			addImportNamespaces(startMap, loc);

			//create temporary OntModel for access to document manager (needed for loading imports)
			//OWL_MEM_RDFS_INF prevents SPIN rules from running
			OntModel ontModel = JenaUtil.createOntologyModel(OntModelSpec.OWL_MEM, JenaUtil.createDefaultModel());
			OntDocumentManager dm = ontModel.getDocumentManager();

			//write mappings to Jena's document manager
			importLocationMap.entrySet().stream().forEach(mapping -> {
				String newuri = mapping.getKey();
				if (newuri.endsWith("#")) {
					newuri = newuri.substring(0, newuri.length() - 1);
				}
				//if there is no mapping yet, the mapping request will return the uri
				if (dm.doAltURLMapping(newuri).equals(newuri)) {
					dm.addAltEntry(newuri, mapping.getValue().getLocation());
				}
			});

			//list mappings
			Iterator<String> iter = dm.listDocuments();
			if (iter.hasNext()) {
				logger.debug("Listing documents mappings:");
				while (iter.hasNext()) {
					String s = iter.next();
					logger.debug(" - {}", s);
				}
			} else {
				logger.debug("No import documents mapped to disk for this ontology");
			}

			//imports should be sorted now, so model can be put into ontmodel
			ontModel.add(m);

			//actually get triples from imported ontologies
			if (!loc.equals(LoadingLocation.NONE)) {
				long oldSize = ontModel.size();
				ontModel.loadImports();
				logger.debug("Imports loaded, old size: {}, new size: {}", oldSize, ontModel.size());
			}

			//remove unnecessary ontology objects (may have been created by loading imports)
			//--find all ontologies except base ontology (the document loaded)
			List<String> ontologies = new ArrayList<>();
			ExtendedIterator<Ontology> onts = ontModel.listOntologies();
			while (onts.hasNext()) {
				Ontology o = onts.next();
				String testURI = baseURI;
				if (testURI==null && semanticFactory.getBaseURI()!=null) {
					testURI = semanticFactory.getBaseURI();
				}
				//list extra ontologies
				if (testURI != null && !testURI.isEmpty() && !o.getURI().equals(testURI.substring(0, testURI.length()-1))) {
					logger.debug("Ontology found in model: {}", o.getURI());
					ontologies.add(o.getURI());
				}
			}
			//--find all ontology statements
			Set<Statement> ontStatements = new HashSet<>();
			ontModel.listStatements().toList().stream().filter(s ->
				ontologies.contains(s.getSubject().toString())).forEach(s -> ontStatements.add(s)
			);
			//--remove them
			ontStatements.stream().filter(s -> ontModel.contains(s)).forEach(s -> {
				logger.debug("Removing ontology statement {}", s.toString());
				ontModel.remove(s);
			});
			ontModel.rebind();

			//write contents of ontModel back to "normal" Jena model for return
			loadedModel = JenaUtil.createDefaultModel().add(ontModel);

			//take care of prefixes separately as this doesn't happen automatically
			loadedModel.setNsPrefixes(semanticFactory.getNamespaces());

		} else {
			logger.debug("Not loading any imports");
		}

		long end = System.currentTimeMillis();
		long time = end - start;

		if (baseURI!=null) {
			logger.info("Ontology <{}> loaded from {} in {}ms, containing {} triples",
					baseURI, loc.toString(), time, loadedModel.size());
		} else {
			logger.info("Ontology loaded from {} in {}ms, containing {} triples. No default namespace.",
					loc.toString(), time, loadedModel.size());
		}

		return loadedModel;
	}

	// Reasoning //////////////////////////////////////////////////////////////////////////////////
	/**
	 * Run SPIN inferencing on the given model (i.e. run all SPIN rules that are attached to classes).
	 *
	 * @param model the model to use
	 * @return the inference model (all inferred triples)
	 */
	public Model runSPINInferences(Model model) {

		//reset tmp model
		tmpModel = JenaUtil.createDefaultModel();
		logger.info("Starting SPIN inference...");
		int newTriplesAmount = runSPINInferences(model, 0, 0);
		logger.info("SPIN inference completed, {} new triples added", newTriplesAmount);
		return tmpModel;
	}

	/**
	 * Run class-level inferencing on the given model.
	 *
	 * @param model the model to use
	 * @param type the type of reasoner to be used
	 * @return the inference model (all inferred triples)
	 */
	public Model runClassLevelInferences(Model model, ReasonerType type) {

		logger.info("Starting class-level inference...");
		InfModel infModel = ModelFactory.createRDFSModel(model);

		//validate model
		ValidityReport validity = infModel.validate();
		if (validity.isValid()) {
			logger.debug("Model is valid");
		} else {
			logger.error("Conflicts in model:");
			for (Iterator<ValidityReport.Report> i = validity.getReports(); i.hasNext();) {
				logger.error(" - {}", i.next());
			}
		}
		logger.debug("Model size before class-level reasoning: {}", model.size());

		//grab reasoner
		Reasoner reasoner;
		switch (type) {
			case TRANSITIVE:
				reasoner = ReasonerRegistry.getTransitiveReasoner();
				break;
			case RDFS_SIMPLE:
				reasoner = ReasonerRegistry.getRDFSSimpleReasoner();
				break;
			case RDFS:
				reasoner = ReasonerRegistry.getRDFSReasoner();
				break;
			case OWL_MICRO:
				reasoner = ReasonerRegistry.getOWLMicroReasoner();
				break;
			case OWL_MINI:
				reasoner = ReasonerRegistry.getOWLMiniReasoner();
				break;
			case OWL:
			default:
				reasoner = ReasonerRegistry.getOWLReasoner();
		}

		//doit
		infModel = ModelFactory.createInfModel(reasoner, model);

		//save all inferred triples to owlModel
		Model addedModel = infModel.getDeductionsModel();
		logger.debug("{} + {} = {}", infModel.getRawModel().size(), addedModel.size(), infModel.size());
		logger.info("Class-level resoning completed, {} new triples inferred.", infModel.size() - addedModel.size());

		return addedModel;
	}

	/**
	 * Run a specific template on the given model
	 *
	 * @param uri the URI of the template to run
	 * @param model the model to use
	 * @return the inference model (all inferred triples)
	 */
	public Model runTemplate(String uri, Model model) {

		return runTemplate(uri, null, model);
	}

	/**
	 * Run a specific template on the given model.
	 *
	 * @param uri the URI of the template to run
	 * @param arguments the arguments used in the template. You have to ensure that they come in the correct format,
	 *					e.g. "someString" or <http://example/com/ont#URI> with the quotes or brackets included.
	 * @param model the model to use
	 * @return the inference model (all inferred triples)
	 */
	public Model runTemplate(String uri, Map<String, String> arguments, Model model) {

		//translate to full URI in case it was relative
		semanticFactory.addMappingsFromModel(model);
		String tUri = semanticFactory.toFullURI(uri);

		//make sure this is not an ontmodel - it messes up the template body
		Template template = SPINModuleRegistry.get().getTemplate(tUri, model);
		if (template == null) {
			logger.error("Trying to execute unknown template <{}>", tUri);
			throw new IllegalArgumentException("Trying to execute unknown template " + tUri);
		}
		return runTemplate(template, arguments, model);
	}

	/**
	 * Run a specific template on the given model.
	 *
	 * @param t the template to run
	 * @param arguments the arguments used in the template
	 * @param model the model to use
	 * @return the inference model (all inferred triples)
	 */
	protected Model runTemplate(Template t, Map<String, String> arguments, Model model) {

		long time;
		long start = System.currentTimeMillis();

		//start with empty model
		Model infModel = JenaUtil.createDefaultModel();

		if (t == null) {
			logger.debug("Template was null, skipping...");
			return infModel;
		}

		logger.debug("Running template {}", t.getLocalName());
		if (t.getBody() != null) {
			try {
				//check arguments exist if required
				if (t.getArgumentsMap() != null && !t.getArgumentsMap().isEmpty()) {
					if (arguments != null) {
						for (Map.Entry<String, Argument> e : t.getArgumentsMap().entrySet()) {
							if (!arguments.containsKey(e.getKey())) {
								logger.error("Error, argument {} missing. Skipping template.", e.getKey());
								return infModel;
							}
						}
					} else {
						logger.error("Expected arguments {} but didn't receive any. Skipping template.",
								t.getArgumentsMap().toString());
						return infModel;
					}
				}

				//init
				SPINModuleRegistry.get().init();
				SPINModuleRegistry.get().registerTemplates(model);
				SPINModuleRegistry.get().registerFunctions(model, null);

				//set arguments
				QuerySolutionMap arqBindings = new QuerySolutionMap();
				if (arguments != null) {
					arguments.entrySet().stream().forEach(e
							-> arqBindings.add(e.getKey(), new ResourceImpl(e.getValue()))
					);
					logger.debug("Arguments: {}", arqBindings.toString());
				}

				//create query
				String query = getSparqlPrefixes(model) + t.getBody().toString();
				//TODO: workaround because the SPIN library isn't doing its job properly
				if (!query.toUpperCase().contains("FILTER NOT EXISTS")) {
					query = query.replace("NOT EXISTS", "FILTER NOT EXISTS");
					query = query.replace("not exists", "filter not exists");
				}

				//replace arguments
				Iterator<String> it = arqBindings.varNames();
				while (it.hasNext()) {
					String varName = it.next();
					query = query.replace("?" + varName, arqBindings.get(varName).toString());
				}

				//doit
				QueryExecution qexec = QueryExecutionFactory.create(query, model);
				//TODO: this is how it should be. once this works, we can replace the code
				//Query arq = ARQFactory.get().createQuery((org.topbraid.spin.model.Query)t.getBody());
				//QueryExecution qexec = ARQFactory.get().createQueryExecution(arq, fullModel, arqBindings);
				//qexec.setInitialBinding(arqBindings);
				//logger.debug(t.getArgumentsMap().toString());
				infModel = qexec.execConstruct();

			} catch (Exception e) {
				logger.error("Error running template <{}>: {}", t.getLocalName(), t.getBody(), e);
			}
		} else {
			logger.error("Template <{}> has no body, can't run", t.getURI());
		}

		long end = System.currentTimeMillis();
		time = end - start;
		logger.info("Finished running template <{}> in {}ms. {} new triples created",
				t.getLocalName(), time, infModel.size());
		return infModel;
	}

	/**
	 * Run all templates  (subclasses of a given template class) by individually loading and executing them.
	 *
	 * @param templateClass the URI of the template class of which to load the templates
	 * @param model the model to use
	 * @return the inference model (all inferred triples)
	 */
	public Model runTemplatesOfClass(String templateClass, Model model) {

		//translate to full URI
		semanticFactory.addMappingsFromModel(model);
		String tClass = semanticFactory.toFullURI(templateClass);
		logger.info("Running all templates of class <{}>", tClass);

		List<Template> templates = new ArrayList<>();
		Collection<Template> allTemplates = SPINModuleRegistry.get().getTemplates();

		//load all SPIN templates
		SPINModuleRegistry.get().init();
		SPINModuleRegistry.get().registerTemplates(model);
		templates.addAll(allTemplates);

		//get all templates of parent template class
		ResultSet result = querySelect("SELECT * WHERE { ?tmp rdfs:subClassOf* <" + tClass + "> . }",
				model, OntModelSpec.OWL_MEM_RDFS_INF);
		//this will collect all the templates' URIs
		Set<String> tmps = new HashSet<>();
		while (result.hasNext()) {
			QuerySolution row = result.next();
			tmps.add(row.get("tmp").toString());
		}

		//filter templates
		allTemplates.stream().forEach(template -> {
			//filter by classname
			if (!tmps.contains(template.getURI()) || template.getURI().equals(tClass)) {
				templates.remove(template);
			} else {
				logger.debug("Template <{}> found", template.getLocalName());
			}
		});
		logger.info("{} template(s) loaded from parent class <{}>", templates.size(), tClass);

		Model infModel = JenaUtil.createDefaultModel();

		//run templates
		templates.stream().forEach(template -> {
			if (template.getBody() != null) {
				//run template without any arguments
				infModel.add(runTemplate(template, null, model));
			} else {
				logger.warn("Skipping template {}, template body is null", template.getLocalName());
			}
		});
		logger.info("Finished running templates, {} new triples added", infModel.size());
		return infModel;
	}

	// Add/remove /////////////////////////////////////////////////////////////////////////////////
	/**
	 * Add the given triple to the model
	 *
	 * @param subject the subject of the triple
	 * @param predicate the predicate of the triple
	 * @param object the object of the triple
	 * @param type @see Triple.TripleType
	 * @param model the input model
	 * @return the model including the added triple
	 */
	public Model addTriple(String subject, String predicate, String object, TripleType type, Model model) {

		semanticFactory.addMappingsFromModel(model);
		return model.add(semanticFactory.createStatement(subject, predicate, object, type));
	}

	/**
	 * Add multiple triples to the model
	 *
	 * @param triples the triples to add
	 * @param model the model to add them to
	 * @return the model containing both: the original model as well as the added triples
	 */
	public Model addTriples(Set<uk.ac.soton.itinnovation.easyjena.core.impl.Triple> triples, Model model) {

		Model m = JenaUtil.createDefaultModel();
		semanticFactory.addMappingsFromModel(model);
		triples.stream().forEach(t
			-> m.add(semanticFactory.createStatement(t.getSubject(), t.getPredicate(), t.getObject(), t.getType()))
		);
		return model.add(m);
	}

	/**
	 * Remove the given triple from the model
	 *
	 * @param subject the subject of the triple
	 * @param predicate the predicate of the triple
	 * @param object the object of the triple
	 * @param type @see Triple.TripleType
	 * @param model the input model
	 * @return the model without the given triple
	 */
	public Model removeTriple(String subject, String predicate, String object, TripleType type, Model model) {

		semanticFactory.addMappingsFromModel(model);
		return model.remove(semanticFactory.createStatement(subject, predicate, object, type));
	}

	/**
	 * Adds an import statement to the model without specifying a prefix
	 *
	 * @param importURI the URI of the import
	 * @param model the model to use
	 * @return the model with the import statement
	 */
	public Model addImport(String importURI, Model model) {

		return addImport(importURI, null, model);
	}

	/**
	 * Adds an import statement to the model and a prefix for the imported URI to the internal map
	 *
	 * @param importURI the URI of the import
	 * @param prefix the prefix to be used for the imported URI
	 * @param model the model to use
	 * @return the model with the import statement and specified prefix
	 */
	public Model addImport(String importURI, String prefix, Model model) {

		semanticFactory.addMappingsFromModel(model);
		//add prefix
		if (prefix!=null) {
			semanticFactory.addPrefixURIMapping(prefix, importURI);
		}

		//add import to model
		model.add(semanticFactory.createStatement(
			//subject: the ontology itself. Cut string as it needs to be without the separator
			semanticFactory.getBaseURI().substring(0, semanticFactory.getBaseURI().length() - 1),
			//predicate: owl:imports
			OWL_IMPORTS,
			//object: the given URI. Cut string as it needs to be without the separator
			importURI.substring(0, importURI.length() - 1),
			//triple type: this is an object property, i.e. linking two objects
			uk.ac.soton.itinnovation.easyjena.core.impl.Triple.TripleType.OBJECT_PROPERTY
		));

		return model;
	}

	/**
	 * Removes an import statement from the model
	 *
	 * @param importURI the URI of the import to remove
	 * @param model the model from which to remove the import
	 * @return the model without the import. The prefix will still be there.
	 */
	public Model removeImport(String importURI, Model model) {

		semanticFactory.addMappingsFromModel(model);

		//remove from model
		model.remove(semanticFactory.createStatement(
			//subject: the ontology itself. Cut string as it needs to be without the separator
			semanticFactory.getBaseURI().substring(0, semanticFactory.getBaseURI().length() - 1),
			//predicate: owl:imports
			OWL_IMPORTS,
			//object: the given URI. Cut string as it needs to be without the separator
			importURI.substring(0, importURI.length() - 1),
			//triple type: this is an object property, i.e. linking two objects
			uk.ac.soton.itinnovation.easyjena.core.impl.Triple.TripleType.OBJECT_PROPERTY
		));

		//leave prefix alone
		return model;
	}

	// Query //////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Run a SPARQL SELECT query on the given model
	 *
	 * @param sparql the SPARQL query without prefixes (prefixes will be loaded automatically from the model)
	 * @param model the model to be queried
	 * @param modelSpec the ontology model specification, @see OntModelSpec
	 * @return the query's resultset or null if there was en error
	 */
	public ResultSet querySelect(String sparql, Model model, OntModelSpec modelSpec) {

		//get prefixes from model
		String sparqlFull = getSparqlPrefixes(model) + "\n" + sparql;
		ResultSet r = null;
		try {
			if (model.isEmpty()) {
				logger.warn("Model is empty, query will not return any results");
			}
			OntModel ontModel = JenaUtil.createOntologyModel(modelSpec, model);
			Query query = QueryFactory.create(sparqlFull);
			QueryExecution qexec = QueryExecutionFactory.create(query, ontModel);
			if (sparql.toUpperCase().startsWith("SELECT")) {
				ResultSet results = qexec.execSelect();
				//deep copy resultset
				r = new ResultSetMem(results);
			} else {
				throw new IllegalArgumentException("Query could not be executed, it doesn't start with SELECT");
			}

		} catch (Exception e) {
			throw new RuntimeException("Error executing SELECT query " + sparqlFull, e);
		}
		return r;
	}

	/**
	 * Executes a SPARQL CONSTRUCT on the given model
	 *
	 * @param sparql the SPARQL CONSTRUCT query without prefixes (prefixes will be loaded automatically from the model)
	 * @param model the model to be queried
	 * @return the resulting model of constructed triples or null if there was en error
	 */
	public Model queryConstruct(String sparql, Model model) {

		Model infModel = null;
		//get prefixes from model
		String sparqlFull = getSparqlPrefixes(model) + "\n" + sparql;
		try {
			Query query = QueryFactory.create(sparqlFull);
			try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
				if (sparql.toUpperCase().startsWith("CONSTRUCT")) {
					infModel = qexec.execConstruct();
					logger.info("{} triples constructed by query", infModel.size());
				} else {
					logger.error("Query could not be executed, it doesn't start with CONSTRUCT");
				}
			}
		} catch (Exception e) {
			logger.error("Error in query {}", sparqlFull, e);
			throw new IllegalArgumentException("Query could not be executed, it doesn't start with CONSTRUCT");
		}
		return infModel;
	}

	/**
	 * Executes a SPARQL DESCRIBE on the given model
	 *
	 * @param sparql the SPARQL DESCRIBE query without prefixes (prefixes will be loaded automatically from the model)
	 * @param model the model to be queried
	 * @return the results of the query or null if there was en error
	 */
	public Model queryDescribe(String sparql, Model model) {

		String sparqlFull = getSparqlPrefixes(model) + "\n" + sparql;
		logger.debug("describe query not yet supported:\n{}", sparqlFull);
		//TODO: implement
		return null;
	}

	/**
	 * Executes a SPARQL ASK on the given model
	 *
	 * @param sparql the SPARQL ASK query (prefixes will be loaded automatically from the model)
	 * @param model the model to be queried
	 * @return the results of the query or null if there was en error
	 */
	public boolean queryAsk(String sparql, Model model) {

		//String sparqlFull = getSparqlPrefixes(model) + "\n" + sparql;
		//TODO: implement
		throw new java.lang.UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Run a SPARQL update (INSERT) on the model.
	 *
	 * @param sparql the SPARQL update without prefixes (prefixes will be loaded automatically from the model)
	 * @param model the model to be used
	 * @return the updated model or the original model if there was en error
	 */
	public Model updateInsert(String sparql, Model model) {

		Model m = update(sparql, model);
		logger.info("{} triples inserted by query", model.size() - m.size());

		return m;
	}

	/**
	 * Run a SPARQL update (DELETE) on the model.
	 *
	 * @param sparql the SPARQL update without prefixes (prefixes will be loaded automatically from the model)
	 * @param model the model to be used
	 * @return the updated model or the original model if there was en error
	 */
	public Model updateDelete(String sparql, Model model) {

		Model m = model;
		if (model.isEmpty()) {
			logger.warn("Model is empty, deleting will not have any effect");
		} else {
			m = update(sparql, model);
			logger.info("{} triples deleted by query", model.size() - m.size());
		}

		return m;
	}

	/**
	 * Executes a SPARQL update on the model. This can be used for combined DELETE/INSERT queries.
	 *
	 * @param sparql the update query without prefixes (prefixes will be loaded automatically from the model)
	 * @param model the model to be used
	 * @return the updated model or the original model if there was en error
	 */
	public Model update(String sparql, Model model) {

		//get prefixes from model
		String sparqlFull = getSparqlPrefixes(model) + "\n" + sparql;
		//create copy of the model to work on
		Model resultModel = model.union(JenaUtil.createDefaultModel());

		try {
			//get hold of ontology model for querying:
			//--build update
			UpdateRequest update = UpdateFactory.create(sparqlFull);
			//--execute update
			UpdateAction.execute(update, resultModel);
			logger.debug("Model size before/after executing update: {}/{}", model.size(), resultModel.size());
		} catch (Exception e) {
			logger.error("Error in update:\n{}", sparqlFull, e);
			resultModel = model;
		}
		return resultModel;
	}

	// Save ///////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Save the given model to a file
	 *
	 * @param model the model to save
	 * @param path the destination file
	 * @param format the format the model will be saved in
	 * @see JenaOntologyManager.ModelFormat
	 */
	public void saveModel(Model model, String path, ModelFormat format) {

		long time;

		try {
			logger.debug("Writing model to file {}", path);
			long start = System.currentTimeMillis();

			//update semantic factory's namespace map (including baseURI)
			semanticFactory.addMappingsFromModel(model);

			//set all the available namespaces to the model so we don't lose any
			model.setNsPrefixes(semanticFactory.getNamespaces());

			logger.debug("Number of triples in model: {}", model.size());

			//make sure file exists
			File outfile = new File(path);
			if (!outfile.isFile()) {
				logger.info("File {} doesn't exist, creating it...", path);
				outfile.createNewFile();
			}

			//write to file, always use UTF-8
			try (OutputStreamWriter osw = new OutputStreamWriter(
				new FileOutputStream(path), Charset.forName("UTF-8").newEncoder()
			)) {
				RDFWriter writer = model.getWriter(format.getText());
				writer.setProperty("xmlbase", semanticFactory.getBaseURI());
				//don't use relative URIs
				writer.setProperty("relativeURIs", "");
				writer.setProperty("showXmlDeclaration", "true");
				writer.write(model, osw, null);

				logger.debug("encoding: {}", osw.getEncoding());
				logger.debug("base URI: {}", semanticFactory.getBaseURI());

				osw.flush();
			}

			long end = System.currentTimeMillis();
			time = end - start;

			logger.info("Saved the model (size {}) to {} in {}ms", model.size(), path, time);

		} catch (IOException e) {
			logger.error("Error saving ontology model", e);
		}
	}

	/**
	 * Writes an xmlfile containing import locations for local copies of ontologies.
	 * It will use the current import location mappings.
	 *
	 * @param xmlpath the location of the XML config file
	 */
	public void saveImportLocationConfigFile(String xmlpath) {

		if (this.importLocationMap.isEmpty()) {
			logger.warn("No import locations defined, won't save empty file {}", xmlpath);
			return;
		}

		DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder icBuilder;
		try {
			icBuilder = icFactory.newDocumentBuilder();
			Document doc = icBuilder.newDocument();
			//create root element
			Element mainRootElement = doc.createElement("configs");
			doc.appendChild(mainRootElement);

			//for each mapping create entry
			for (Map.Entry<String, ImportMapping> e : this.importLocationMap.entrySet()) {
				Element imp = doc.createElement("file");

				Element uri = doc.createElement("uri");
				uri.appendChild(doc.createTextNode(e.getKey()));
				imp.appendChild(uri);

				Element path = doc.createElement("path");
				path.appendChild(doc.createTextNode(e.getValue().getLocation()));
				imp.appendChild(path);

				//determine loading type, follow same priority as in mapping imports
				String type = null;
				if (e.getValue().getType().getNumVal()>=LoadingLocation.DIRECTORY.getNumVal()) {
					type = "disk";
				} else if (e.getValue().getType().getNumVal()%2!=0) {
					type = "web";
				} else if (e.getValue().getType().equals(LoadingLocation.CLASSPATH)
						|| e.getValue().getType().equals(LoadingLocation.WEB_AND_CLASSPATH)
						|| e.getValue().getType().equals(LoadingLocation.CLASSPATH_AND_DIRECTORY)
						|| e.getValue().getType().equals(LoadingLocation.ALL)) {
					type = "classpath";
				}
				//write element if valid
				if (type !=null) {
					Element location = doc.createElement("location");
					location.appendChild(doc.createTextNode(type));
					imp.appendChild(location);
				}

				//append this mapping to root element
				mainRootElement.appendChild(imp);
			}

			// output DOM XML to console
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			StreamResult output = new StreamResult(new File(xmlpath));
			transformer.transform(source, output);

			logger.info("Saved import location config file to {}", xmlpath);

		} catch (ParserConfigurationException | DOMException | IllegalArgumentException | TransformerException e) {
			logger.error("Could not write import location config file to {}", xmlpath, e);
		}
	}

	// Get ////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Gets the simple triple representation of a model (similar to turtle; just a flat list of triples)
	 *
	 * @param model the model
	 * @param baseOnly only consider triples from the base ontology namespace
	 * @return the triples, line by line for printing
	 */
	public String getSimpleTripleString(Model model, boolean baseOnly) {

		StringBuilder modelString = new StringBuilder();

		StmtIterator triples = model.listStatements();
		while (triples.hasNext()) {
			Triple t = triples.next().asTriple();

			String subject = t.getSubject().toString();
			if (t.getSubject().isURI()) {
				subject = model.getNsURIPrefix(t.getSubject().getNameSpace()) + ":" + t.getSubject().getLocalName();
			}
			String predicate = model.getNsURIPrefix(t.getPredicate().getNameSpace()) + ":"
					+ t.getPredicate().getLocalName();
			String object = t.getObject().toString();
			if (t.getObject().isURI()) {
				object = model.getNsURIPrefix(t.getObject().getNameSpace()) + ":" + t.getObject().getLocalName();
			} else if (t.getObject().isLiteral()) {
				object = t.getObject().getLiteralValue().toString();
			}

			//filter by namespace
			semanticFactory.addMappingsFromModel(model);
			if (baseOnly && semanticFactory.getBaseURI() != null) {
				if (t.getSubject().toString().startsWith(semanticFactory.getBaseURI())
						|| (!t.getObject().isLiteral()
						&& t.getObject().toString().startsWith(semanticFactory.getBaseURI()))) {
					modelString.append(String.join(subject, "\t", predicate, "\t", object, "\n"));
				}
			} else {
				modelString.append(String.join(subject, "\t", predicate, "\t", object, "\n"));
			}
		}
		return modelString.toString();
	}

	/**
	 * Gets the RDF representation of a model
	 *
	 * @param model the model
	 * @param format the format in which to retrieve the string representation of this model
	 * @return the RDF representation of the model
	 * @see JenaOntologyManager.ModelFormat
	 */
	public String getModelString(Model model, ModelFormat format) {

		StringWriter writer = new StringWriter();
		model.write(writer, format.getText());
		return writer.toString();
	}

	/**
	 * Gives general information about this ontology manager
	 *
	 * @return prefix- and import type map information
	 */
	@Override
	public String toString() {

		return "JenaOntologyManager:"
		+ "\n\t prefixURIMap:    " + semanticFactory.getNamespaces().toString()
		+ "\n\t prefixImportMap: " + importLocationMap.entrySet().toString()
		+ "\n\t properties:      " + props.toString();
	}

	/**
	 * Get hold of an OntModel for easier adding of new triples.
	 *
	 * @param model the model to use
	 * @param spec what kind of inferencing
	 * @return an OntModel based on the current model
	 * @see com.hp.hpl.jena.ontology.OntModel
	 */
	public OntModel getOntModel(Model model, OntModelSpec spec) {

		return JenaUtil.createOntologyModel(spec, model);
	}

	/**
	 * Retrieve all imports of this ontology model
	 *
	 * @param model the model
	 * @return a set of URIs describing the imported ontologies
	 */
	public Set<String> getImportedOntologyURIs(Model model) {

		Set<String> set = new HashSet<>();
		semanticFactory.addMappingsFromModel(model);
		if (semanticFactory.getBaseURI() != null && !semanticFactory.getBaseURI().isEmpty()) {
			//need to cut last character, whether it's # or /
			ResultSet result = querySelect("SELECT ?import WHERE { <"
			+ semanticFactory.getBaseURI().substring(0, semanticFactory.getBaseURI().length() - 1)
			+ "> <" + OWL_IMPORTS + "> ?import }", model, OntModelSpec.RDFS_MEM);
			if (result != null) {
				while (result.hasNext()) {
					QuerySolution row = result.next();
					logger.debug("Import found: {}", row.get("import"));
					set.add(row.get("import").toString());
				}
			}
		} else {
			//TODO: get imports without base uri
		}
		return set;
	}

	/**
	 * Retrieves a list of all available prefixes. This can be used for creating
	 * SPARQL queries so all prefixes can be used without having to be defined explicitly.
	 *
	 * @param model the input model
	 * @return the prefixes
	 */
	public String getSparqlPrefixes(Model model) {

		semanticFactory.addMappingsFromModel(model);
		String prefixes = "";
		prefixes = semanticFactory.getNamespaces().entrySet().stream().map(
				e -> "PREFIX " + e.getKey() + ":<" + e.getValue() + ">\n").reduce(prefixes, String::concat);
		return prefixes;
	}

	// PRIVATE METHODS ////////////////////////////////////////////////////////////////////////////
	/**
	 * Runs the SPIN inferences recursively until no further triples are added to the model. This is
	 * a workaround because the native method to do this gets stuck in an infinite loop.
	 *
	 * @param iteration the current iteration
	 * @param newTriplesAmount the amount of new triples found in previous iterations
	 * @return the amount of new triples altogether after the current iteration
	 */
	private int runSPINInferences(Model model, int iteration, int newTriplesAmount) {

		int it = iteration + 1;
		int newTriplesNum = newTriplesAmount;

		if (it > MAX_SPIN_ITERATIONS) {
			logger.warn("{} iterations reached; there might be an infinite loop in the SPIN rules. Stopping now."
					, MAX_SPIN_ITERATIONS);
			return newTriplesNum;
		}

		if (model == null) {
			logger.error("model is null! Can't reason on an empty model.");
			return 0;
		}

		logger.info("Running SPIN inference, iteration {}", it);
		logger.debug("Model size before SPIN reasoning: {}", model.size());
		//logger.debug("Model type: " + model.getClass().toString());

		//reset SPINModuleRegistry (this operation will remove registered OWL-RL and improve performances
		SPINModuleRegistry.get().init();
		//model "source code" should be sufficient as it contains all the spin rules already
		SPINModuleRegistry.get().registerAll(model, null);

		Model tmpSpinModel = ModelFactory.createDefaultModel();
		try {
			//runonce=false didn't finish, so iterative reasoning was implemented manually
			SPINInferences.run(model, tmpSpinModel, null, null, true, null);
		} catch (ResourceRequiredException e) {
			//TODO: find a way to fix/prevent this
			logger.warn("Error processing SPIN rule related to {}, ignore and continue", e);
		} catch (Exception e) {
			logger.error("Error running SPIN inferences", e);
		}

		tmpModel.add(tmpSpinModel);

		// Run all constraints
		List<ConstraintViolation> cvs = SPINConstraints.check(model, null);
		if (cvs.isEmpty()) {
			logger.debug("No constraint violations found");
		} else {
			logger.warn("Constraint violations: {}", cvs.size());
//			for (ConstraintViolation cv : cvs) {
//				logger.warn(" - at {}: {}", SPINLabels.get().getLabel(cv.getRoot()), cv.getMessage());
//			}
		}

		logger.debug("Model size after SPIN reasoning: {}", model.size());
		logger.debug("{} triples added", tmpSpinModel.size());
		newTriplesNum += tmpSpinModel.size();

		if (!tmpSpinModel.isEmpty()) {
			logger.debug("New triples inferred in iteration {}, running again", it);
			newTriplesNum = runSPINInferences(model, it, newTriplesNum);
		}
		return newTriplesNum;
	}

	/**
	 * Recursively searches all the prefixes and adds them to the internal map, using the mappings
	 * to files on disk if they exist.
	 *
	 * @param uriMap the NSPrefixMap of the current ontology
	 */
	private void addImportNamespaces(Map<String, String> uriMap, LoadingLocation location) {

		try {
			Iterator<Map.Entry<String, String>> it = uriMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, String> e = it.next();
				//it.remove();
				//skip unloadable prefixes (timeout)
				//TODO: better solution for breaking off at an invalid XML document?
				if ("xsd".equals(e.getKey()) || "fn".equals(e.getKey()) || "xml".equals(e.getKey())) {
					continue;
				}
				logger.debug("Add namespaces from import {}", e.toString());
				String path;
				//is there a mapping for this import?
				if (importLocationMap.containsKey(e.getValue())) {
					//local location
					path = importLocationMap.get(e.getValue()).getLocation();
					//logger.debug("Key {} contained, location {}", e.getKey(), location);
				} else {
					//web address
					path = e.getValue();
					//logger.debug("Key {} not contained, location {}", e.getKey(), location);
				}

				//add to semantic factory if the file can be loaded locally or from URL (considering location!)
				if ((new File(path)).exists() || (location.getNumVal()%2!=0 && urlExists(path))) {

					//actually load the import to look at its defined prefixes and imports
					Model m = FileManager.get().loadModel(path);

					//skip prefixes already processed in an earlier import
					for (Map.Entry<String, String> e2: m.getNsPrefixMap().entrySet()) {
						if (!semanticFactory.containsPrefix(e2.getKey()) && !e2.getKey().isEmpty()) {
							//add this import's prefixes to the collection
							//note that one URI can have multiple prefixes but not the other way round.
							logger.debug("Adding new prefix {}", e2);
							semanticFactory.addPrefixURIMapping(e2.getKey(), e2.getValue());
						}
					}

					//recurse to retrieve the full tree of namespaces
					//only recurse into true imports here, not defined prefixes
					Map<String, String> importMap = getImports(m, true);
					if (!importMap.isEmpty()) {
						addImportNamespaces(importMap, location);
					}

				} else {
					logger.debug("Skipping {}: {} can not be opened", e.getKey(), path);
					//only add this prefix and ignore its imports
					semanticFactory.addPrefixURIMapping(e.getKey(), e.getValue());
				}
			}
		} catch (Exception e) {
			logger.error("Error adding import namespaces", e);
		}
	}

	/**
	 * Checks whether the given URL exists and represents a valid ontology file
	 *
	 * @param location the URL to test
	 * @return whether it exists and is valid or not
	 */
	private boolean urlExists(String location) {

		boolean found = false;
		try {
			//Check if the URL is valid, i.e. exists
			URL url = new URL(location);
			HttpURLConnection huc = (HttpURLConnection) url.openConnection();
			huc.setRequestMethod("GET");
			//don't actually load the file, only connect so the HTTP resonse code can be retrieved
			huc.connect();
			int responseCode = huc.getResponseCode();
			String responseMsg = huc.getResponseMessage();

			//if the URI exists
			if (responseCode == 200) {

				//load file and check whether it's in an allowed format
				String contentType = huc.getContentType();
				//logger.debug("Content type: {}", contentType);
				String[] allowedContentTypes = {"application/rdf+xml;", "text/turtle", "text/plain", "text/html"};
				for (String act : allowedContentTypes) {
					if (contentType.contains(act)) {
						found = true;
						break;
					}
				}

				//TODO: open file and do sanity check and if it fails set found to false again

				if (!found) {
					logger.debug("Wrong content type ({}): the document found at URL <{}> " +
							"is not a valid ontology file", contentType, location);
				}

			//HTTP error encountered
			} else {
				logger.debug("Could not find ontology at given URL, HTTP error code: {} - {}", responseCode, responseMsg);
			}

		} catch (MalformedURLException e) {
			logger.debug("URL <{}> is invalid", location, e);
		} catch (IOException e) {
			logger.debug("Error loading ontology from URL <{}>", location, e);
		}
		return found;
	}

	/**
	 * Retrieve a list of all the imports from the given model, which have a defined prefix
	 * @param m the model to scan
	 * @param newOnly return all imports or only new ones (that don't exist in the semantic factory yet)
	 * @return a map of new imports and their prefixes
	 */
	private Map<String, String> getImports(Model m, boolean newOnly) {
		Map<String, String> importMap = new HashMap<>();
		Map<String, String> fullMap = m.getNsPrefixMap();
		NodeIterator imports = m.listObjectsOfProperty(new PropertyImpl(OWL_IMPORTS));
		while (imports.hasNext()) {
			RDFNode imp = imports.next();
			logger.debug("Import found: {}", imp);
			for (Map.Entry<String, String> entry: fullMap.entrySet()) {
				//find the import's prefix in the model's mapping...
				if (entry.getValue().startsWith(imp.toString())
						//...but only proceed if this import issupposed to be loaded
						&& (!newOnly || !semanticFactory.containsPrefix(entry.getKey()))) {
					logger.debug("Using prefix {} for import <{}>", entry.getKey(), entry.getValue());
					importMap.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return importMap;
	}

	// GETTERS / SETTERS //////////////////////////////////////////////////////////////////////////
	/**
	 * Returns a mapping between URIs (long prefix) and the type on disk from which the ontology
 should be loaded.
	 *
	 * @return the mapping
	 */
	public Map<String, ImportMapping> getImportLocationMap() {
		return importLocationMap;
	}

	/**
	 * Set a new map for import locations
	 *
	 * @param importLocationMap the new map
	 */
	public void setImportLocationMap(Map<String, ImportMapping> importLocationMap) {
		this.importLocationMap = importLocationMap;
	}

	/**
	 * Returns a mapping between short prefixes and URIs
	 *
	 * @return the mapping
	 */
	public Map<String, String> getPrefixURIMap() {
		return semanticFactory.getNamespaces();
	}

	/**
	 * Set a new prefix map
	 *
	 * @param prefixURIMap the new map
	 */
	public void setPrefixURIMap(Map<String, String> prefixURIMap) {
		this.semanticFactory.setNamespaces(prefixURIMap);
	}

	/**
	 * Get the current base namespace for this manager
	 *
	 * @return the base URI
	 */
	public String getBaseURI() {
		return semanticFactory.getBaseURI();
	}

	/**
	 * Get the properties object used for this manager
	 *
	 * @return the properties
	 */
	public Properties getProps() {
		return props;
	}

	/**
	 * Set new properties for use in this manager. Includes validity check.
	 *
	 * @param props the new properties
	 */
	public final void setProps(Properties props) {
		this.props = props;

		if (!props.containsKey(SOURCE_PATH)) {
			logger.info("No {} defined. Using default (empty) source path.", SOURCE_PATH);
			props.setProperty(SOURCE_PATH, "");
		}
	}

	/**
	 * Set a default loading directory for ontology imports
	 *
	 * @param path the directory's location on disk
	 */
	public void setDirectory(String path) {

		if (this.getProps() == null) {
			setProps(new Properties());
		}
		this.getProps().setProperty(SOURCE_PATH, path);
	}

	/**
	 * Set any property for use in this manager
	 *
	 * @param key the property text
	 * @param value the property value
	 */
	public void setProperty(String key, String value) {
		if (this.getProps() == null) {
			setProps(new Properties());
		}
		this.getProps().setProperty(key, value);
	}

	/**
	 * Get hold of the semantic factory used in this ontology manager
	 *
	 * @return the semantic factory
	 */
	public SemanticFactory getSemanticFactory() {
		return semanticFactory;
	}

	// ENUM	///////////////////////////////////////////////////////////////////////////////////////

	/**
	 * This enum describes the possible locations from which ontology imports can be loaded. Note
	 * that each individual type has a number and the combined locations are the numbers of the
	 * individual locations added up. The order is explicitly mapped ontologies > web > directory > classpath
	 *
	 * (0) NONE - only use explicitly added import locations
	 * (1) WEB - try to load from an ontology's URL on the internet
	 * (2) CLASSPATH - try to find the ontology document on the classpath. This is greedy in case more than one file
	 *					share the same name.
	 * (4) DIRECTORY - lazy local ontology loading from a directory and its subdirs
	 * (3) WEB_AND_CLASSPATH - 1 and 2
	 * (5) WEB_AND_DIRECTORY - 1 and 4
	 * (6) CLASSPATH_AND_DIRECTORY - 2 and 4
	 * (7) ALL;	- try all possibilities
	 */
	public enum LoadingLocation {

		NONE(0),
		WEB(1),
		CLASSPATH(2),
		DIRECTORY(4),
		WEB_AND_CLASSPATH(3),
		WEB_AND_DIRECTORY(5),
		CLASSPATH_AND_DIRECTORY(6),
		ALL(7);

		private final int numVal;

		LoadingLocation(int numVal) {
			this.numVal = numVal;
		}

		public int getNumVal() {
			return numVal;
		}

		@Override
		public String toString() {
			return this.name();
		}
	}

	/**
	 * This enum holds a list of all the different reasoner types supported by JenaOntologyManager.
	 *
	 * @see com.hp.hpl.jena.reasoner.ReasonerRegistry
	 */
	public enum ReasonerType {

		//Prebuilt standard configuration for the default subclass/subproperty transitive closure reasoner.
		TRANSITIVE,
		//Prebuilt simplified configuration for the default RDFS reasoner
		RDFS_SIMPLE,
		//Prebuilt standard configuration for the default RDFS reasoner
		RDFS,
		//Prebuilt standard configuration a micro-OWL reasoner.
		OWL_MICRO,
		//Prebuilt mini configuration for the default OWL reasoner.
		OWL_MINI,
		//Prebuilt standard configuration for the default OWL reasoner.
		OWL
	}

	/**
	 * Different formats for (de-)serialisation
	 *
	 * @see https://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/RDFWriter.html
	 */
	public enum ModelFormat {

		N3("N3"),
		N_TRIPLE("N_TRIPLE"),
		RDF_XML("RDF/XML"),
		RDF_XML_ABBREV("RDF/XML-ABBREV");

		private final String text;

		ModelFormat(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}
	}

}
