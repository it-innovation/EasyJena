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
//      Created Date :          2014-12-04
//      Created for Project :   OPTET
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.easyjena.core.spec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides the basis for store wrapper implementations.
 * A store wrapper is a connection to *one* store.
 *
 * @author Stefanie Wiegand
 */
public abstract class AStoreWrapper implements IStoreWrapper {

	private static final Logger logger = LoggerFactory.getLogger(AStoreWrapper.class);

	protected Properties props = new Properties();
	protected Map<String, String> prefixURIMap = new HashMap<>();
	protected String sparqlPrefixes = "";
	protected boolean connected = false;
	protected boolean geoSupport = false;

	/**
	 * It is highly recommended to call this constructor in any implementing classes' constructor.
	 * This preloads a number of commonly used prefixes for easier querying.
	 */
	protected AStoreWrapper() {
		//add a couple of standard prefixes - also see JenaOntologyManager
		prefixURIMap.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		prefixURIMap.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		prefixURIMap.put("xsd", "http://www.w3.org/2001/XMLSchema#");
		prefixURIMap.put("spin", "http://spinrdf.org/spin#");
		prefixURIMap.put("sp", "http://spinrdf.org/sp#");
		prefixURIMap.put("owl", "http://www.w3.org/2002/07/owl#");
		prefixURIMap.put("fn", "http://www.w3.org/2005/xpath-functions#");
		prefixURIMap.put("spl", "http://spinrdf.org/spl#");

		//init prefix map in case it's not loaded from store before first query
		prefixURIMap.entrySet().stream().filter(e -> e.getKey() != null && e.getValue() != null).forEach(e
				-> sparqlPrefixes += "PREFIX " + e.getKey() + ":<" + e.getValue() + ">\n"
		);
	}

	// General actions ////////////////////////////////////////////////////////////////////////////

	@Override
	public Object querySelect(String sparql) {
		logger.error("SELECT queries currently not supported for store of type {}", this.getClass());
		return null;
	}

	@Override
	public Object queryConstruct(String sparql) {
		logger.error("CONSTRUCT queries currently not supported for store of type {}", this.getClass());
		return null;
	}

	@Override
	public Object queryDescribe(String sparql) {
		logger.error("DESCRIBE queries currently not supported for store of type {}", this.getClass());
		return null;
	}

	@Override
	public boolean queryAsk(String sparql) {
		logger.error("ASK queries currently not supported for store of type {}", this.getClass());
		return false;
	}

	@Override
	public void update(String sparql) {
		logger.error("UPDATE queries currently not supported for store of type {}", this.getClass());
	}

	@Override
	public List<Map<String, String>> translateSelectResult(Object results) {
		logger.error("Translating SELECT results has not yet been implemented for store of type {}", this.getClass());
		return null;
	}

	// Getters/Setters ////////////////////////////////////////////////////////////////////////////
	@Override
	public String getSPARQLPrefixes() {
		return sparqlPrefixes;
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public Map<String, String> getPrefixURIMap() {
		return prefixURIMap;
	}

	@Override
	public Properties getProperties() {
		return props;
	}

	@Override
	public boolean hasGeoSupport() {
		return geoSupport;
	}

}
