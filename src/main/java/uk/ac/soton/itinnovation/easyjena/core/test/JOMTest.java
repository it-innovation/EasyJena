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
//		Created Date :			2014-02-20
//		Created for Project:		OPTET
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.easyjena.core.test;

import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.easyjena.core.impl.JenaOntologyManager;

/**
 * This class illustrates the basic usage of EasyJena.
 */
public class JOMTest {

	private static final Logger logger = LoggerFactory.getLogger(JOMTest.class);
	private final Properties props = new Properties();

	/**
	 * Run the test
	 */
	public void run() {

		//optionally load properties file
		try {
			logger.info("Loading properties file");
			props.load(getClass().getClassLoader().getResourceAsStream("easyjena.properties"));
		} catch (IOException e) {
			logger.error("Error loading properties file", e);
		}

		//create ontology manager
		JenaOntologyManager jom = new JenaOntologyManager(props);

		try {
			//add mappings
			jom.addImportLocationMapping("http://example.com/ontologies/example#", "/path/to/ontology.rdf",
					JenaOntologyManager.LoadingLocation.ALL);
			jom.loadImportLocationConfigFile(new File("/path/to/ontology/ImportLocationConfig.xml").getPath());

			//load model
			Model myModel = jom.loadOntology("http://xmlns.com/foaf/0.1/", JenaOntologyManager.LoadingLocation.ALL);

			//inference
			myModel.add(jom.runSPINInferences(myModel));
			myModel.add(jom.runClassLevelInferences(myModel, JenaOntologyManager.ReasonerType.RDFS));
			myModel.add(jom.runTemplate("http://example.com#myTemplate", myModel));
			myModel.add(jom.runTemplatesOfClass("http://example.com#myTemplateClass", myModel));

			//run a template with spin arguments
			Map<String, String> arguments = new HashMap<>();
			arguments.put("foo", "http://example.com#bar");
			myModel.add(jom.runTemplate("http://example.com#myTemplateWithArgs", arguments, myModel));

			//SPARQL
			myModel.add(jom.update("INSERT DATA { foaf:Test rdf:comment \"TestComment\" }", myModel));
			myModel.add(jom.queryConstruct("CONSTRUCT { ?s rdf:comment \"Comment\" } WHERE { ?s ?p ?o }", myModel));

			String query = "SELECT * WHERE {\n" +
			"?s rdfs:subClassOf* owl:Class .\n" +
			"FILTER(!isBlank(?s)) .\n" +
			"}";
			logger.info("\n" + ResultSetFormatter.asText(
					jom.querySelect(query, myModel, OntModelSpec.OWL_MEM_RDFS_INF)
			));

			//save to file
			jom.saveModel(myModel, "/path/to/dir/test.ttl", JenaOntologyManager.ModelFormat.N3);

		} catch (FileNotFoundException e) {
			logger.error("Ontology not found", e);
		}
	}

	/**
	 * Main method
	 * @param args arguments (not used in this main method)
	 */
	public static void main(String[] args) {
		(new JOMTest()).run();
	}
}
