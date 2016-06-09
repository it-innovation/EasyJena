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
//		Created Date :			2014-12-12
//		Created for Project:		REVEAL
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.easyjena.core.test;

import java.io.IOException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.easyjena.core.impl.HttpStoreWrapper;

/**
 * This class illustrates the use of the HTTP connector to access a store's SPARQL endpoint.
 */
public class HttpTest {

	private static final Logger logger = LoggerFactory.getLogger(HttpTest.class);
	private final Properties props = new Properties();

	/**
	 * Run this test
	 */
	public void run() {

		//load properties file which needs to contain the SPARQL endpoint URLs
		try {
			logger.info("Loading properties file");
			props.load(getClass().getClassLoader().getResourceAsStream("easyjena.properties"));
		} catch (IOException e) {
			logger.error("Error loading properties file", e);
		}

		//create store wrapper
		HttpStoreWrapper store = new HttpStoreWrapper(props);

		try {

			String update = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
					+ "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n"
					+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
					+ "PREFIX example:<http://www.it-innovation.soton.ac.uk/example/>\n"
					+ "INSERT DATA {GRAPH <http://www.it-innovation.soton.ac.uk/example/> {\n"
					+ "example:Animal rdf:type owl:Class ."
					+ "}}";
			store.update(update);

			String query = "SELECT * WHERE { ?s ?p ?o . }";
			logger.debug("Result: {}", store.querySelect(query));

		} catch (Exception e) {
			logger.error("Could not run test", e);
		}
	}

	/**
	 * Main method
	 * @param args arguments (not used in this main method)
	 */
	public static void main(String[] args) {
		(new HttpTest()).run();
	}
}
