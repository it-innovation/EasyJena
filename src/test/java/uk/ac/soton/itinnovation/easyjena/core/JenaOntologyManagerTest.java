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
//      Created Date :          2014-10-16
//      Created for Project :   OPTET
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.easyjena.core;

import com.hp.hpl.jena.rdf.model.Model;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.easyjena.core.impl.JenaOntologyManager;
import uk.ac.soton.itinnovation.easyjena.core.impl.JenaOntologyManager.LoadingLocation;

@RunWith(JUnit4.class)
public class JenaOntologyManagerTest extends TestCase
{
    private JenaOntologyManager jom;
    private static Logger logger;
	private static Properties props;

	@Rule public TestName name = new TestName();

    @BeforeClass
    public static void beforeClass() {
		logger = LoggerFactory.getLogger(JenaOntologyManagerTest.class);
		props = getProperties();
        logger.info("JenaOntologyManager tests executing...");
    }

    @Before
    public void beforeEachTest() {
		try {
			jom = new JenaOntologyManager(props);
		} catch (Exception e) {
			logger.error("Error creating JenaOntologyManager prior to test", e);
		}
    }

	@After
	public void afterEachTest() {
		try {
			if (jom !=null) {
				jom = null;
			}
		} catch (Exception e) {
			logger.error("Error destroying JenaOntologyManager", e);
		}
	}

    public static Properties getProperties() {
        props = new Properties();
        try {
            props.load(JenaOntologyManagerTest.class.getClassLoader().getResourceAsStream("easyjena.properties"));
        } catch (IOException ex) {
            logger.error("Error with loading properties file", ex);
            return null;
        }
        return props;
    }

	// Tests //////////////////////////////////////////////////////////////////////////////////////

	// Loading without imports
	/**
	 * Load an ontology from an absolute path
	 */
	@Test
	public void testLoadOntologyFromAbsolutePathNoImports() {
		logger.info("Running test {}", name.getMethodName());
		try {
			jom.addImportLocationMapping("http://www.w3.org/2002/07/owl#",
					getClass().getClassLoader().getResource("test/owl.ttl").getFile(), LoadingLocation.DIRECTORY);
			Model ont = jom.loadOntology("http://www.w3.org/2002/07/owl#", LoadingLocation.NONE);
			if (ont.size()!=450) {
				fail("Loaded ontology size mismatch");
			}
		} catch (Exception e) {
			logger.error("Error loading ontology from absolute path without imports", e);
			fail("Error loading ontology from absolute path without imports");
		}
	}

	/**
	 * Load an ontology from a relative path using the source path option
	 */
	@Test
	public void testLoadOntologyFromRelativePathNoImports() {
		logger.info("Running test {}", name.getMethodName());
		try {
			jom.setDirectory(getClass().getClassLoader().getResource("test").getFile());
			jom.addImportLocationMapping("http://www.w3.org/2002/07/owl#", "owl.ttl", LoadingLocation.DIRECTORY);
			Model ont = jom.loadOntology("http://www.w3.org/2002/07/owl#", LoadingLocation.NONE);
			if (ont.size()!=450) {
				fail("Loaded ontology size mismatch");
			}
		} catch (Exception e) {
			logger.error("Error loading ontology from a relative path without imports", e);
			fail("Error loading ontology from relative path without imports");
		}
	}

	/**
	 * Load an ontology from the classpath using a relative filename
	 */
	@Test
	public void testLoadOntologyFromClasspathNoImports() {
		logger.info("Running test {}", name.getMethodName());
		try {
			jom.addImportLocationMapping("http://www.w3.org/2002/07/owl#", "test/owl.ttl", LoadingLocation.CLASSPATH);
			Model ont = jom.loadOntology("http://www.w3.org/2002/07/owl#", LoadingLocation.NONE);
			if (ont.size()!=450) {
				fail("Loaded ontology size mismatch");
			}
		} catch (Exception e) {
			logger.error("Error loading ontology from classpath without imports", e);
			fail("Error loading ontology from classpath without imports");
		}
	}

	/**
	 * Load an ontology from the internet without loading imports
	 */
	@Test
	public void testLoadOntologyFromWebNoImports() {
		logger.info("Running test {}", name.getMethodName());
		try {
			Model ont = jom.loadOntology("http://www.w3.org/2002/07/owl#", LoadingLocation.NONE);
			if (ont.size()!=450) {
				fail("Loaded ontology size mismatch");
			}
		} catch (Exception e) {
			logger.error("Error loading ontology from web without imports", e);
			fail("Error loading ontology from web without imports");
		}
	}


	// Loading with imports
	/**
	 * Load an ontology from an absolute path
	 */
	@Test
	public void testLoadOntologyFromAbsolutePathWithImports() {
		logger.info("Running test {}", name.getMethodName());
		try {
			jom.addImportLocationMapping("http://www.w3.org/2002/07/owl#",
					getClass().getClassLoader().getResource("test/owl.ttl").getFile(), LoadingLocation.DIRECTORY);
			Model ont = jom.loadOntology("http://www.w3.org/2002/07/owl#", LoadingLocation.WEB);
			if (ont.size()!=537) {
				fail("Loaded ontology size mismatch");
			}
		} catch (Exception e) {
			logger.error("Error loading ontology from absolute path with imports", e);
			fail("Error loading ontology from absolute path with imports");
		}
	}

	/**
	 * Load an ontology from a relative path using the source path option
	 */
	@Test
	public void testLoadOntologyFromRelativePathWithImports() {
		logger.info("Running test {}", name.getMethodName());
		try {
			jom.setDirectory(getClass().getClassLoader().getResource("test").getFile());
			jom.addImportLocationMapping("http://www.w3.org/2002/07/owl#", "owl.ttl", LoadingLocation.DIRECTORY);
			Model ont = jom.loadOntology("http://www.w3.org/2002/07/owl#", LoadingLocation.WEB);
			if (ont.size()!=537) {
				fail("Loaded ontology size mismatch");
			}
		} catch (Exception e) {
			logger.error("Error loading ontology from a relative path with imports", e);
			fail("Error loading ontology from relative path with imports");
		}
	}

	/**
	 * Load an ontology from the classpath using a relative filename
	 */
	@Test
	public void testLoadOntologyFromClasspathWithImports() {
		logger.info("Running test {}", name.getMethodName());
		try {
			jom.addImportLocationMapping("http://www.w3.org/2002/07/owl#", "test/owl.ttl", LoadingLocation.CLASSPATH);
			Model ont = jom.loadOntology("http://www.w3.org/2002/07/owl#", LoadingLocation.WEB);
			if (ont.size()!=537) {
				fail("Loaded ontology size mismatch");
			}
		} catch (Exception e) {
			logger.error("Error loading ontology from classpath with imports", e);
			fail("Error loading ontology from classpath with imports");
		}
	}

	/**
	 * Load an ontology from the internet without loading imports
	 */
	@Test
	public void testLoadOntologyFromWebWithImports() {
		logger.info("Running test {}", name.getMethodName());
		try {
			Model ont = jom.loadOntology("http://www.w3.org/2002/07/owl#", LoadingLocation.WEB);
			if (ont.size()!=537) {
				fail("Loaded ontology size mismatch");
			}
		} catch (Exception e) {
			logger.error("Error loading ontology from web with imports", e);
			fail("Error loading ontology from web with imports");
		}
	}

	// Error tests

	/**
	 * Load an ontology from an absolute path that doesn't exist
	 */
	@Test
	public void testLoadOntologyFromAbsolutePathMappingError() {
		logger.info("Running test {}", name.getMethodName());
		try {
			jom.addImportLocationMapping("http://example.org/myOntology#",
					"/non-existentDir/owl.ttl", LoadingLocation.DIRECTORY);
			jom.loadOntology("http://example.org/myOntology#", LoadingLocation.NONE);
		} catch (FileNotFoundException e) {
			logger.debug("Ontology not found, this is what was expected", e);
		} catch (Exception e) {
			logger.error("Unexpected failure", e);
			fail("Unexpected failure");
		}
	}

	/**
	 * Load an ontology from an relative path that doesn't exist
	 */
	@Test
	public void testLoadOntologyFromRelativePathMappingError() {
		logger.info("Running test {}", name.getMethodName());
		try {
			jom.setDirectory("/non-existentDir/");
			jom.addImportLocationMapping("http://example.org/myOntology#", "owl.ttl", LoadingLocation.DIRECTORY);
			jom.loadOntology("http://example.org/myOntology#", LoadingLocation.NONE);
		} catch (FileNotFoundException e) {
			logger.debug("Ontology not found, this is what was expected", e);
		} catch (Exception e) {
			logger.error("Unexpected failure", e);
			fail("Unexpected failure");
		}
	}

	/**
	 * Load an ontology from classpath path that doesn't exist
	 */
	@Test
	public void testLoadOntologyFromClasspathMappingError() {
		logger.info("Running test {}", name.getMethodName());
		try {
			jom.addImportLocationMapping("http://example.org/myOntology#", "non-existentDir/owl.ttl", LoadingLocation.CLASSPATH);
			jom.loadOntology("http://example.org/myOntology#", LoadingLocation.NONE);
		} catch (FileNotFoundException e) {
			logger.debug("Ontology not found, this is what was expected", e);
		} catch (Exception e) {
			logger.error("Unexpected failure", e);
			fail("Unexpected failure");
		}
	}

	/**
	 * Load an ontology from a URL that doesn't exist
	 */
	@Test
	public void testLoadOntologyFromWebMappingError() {
		logger.info("Running test {}", name.getMethodName());
		try {
			jom.addImportLocationMapping("http://example.org/myOntology#",
					"http://www.example.com/owl#", LoadingLocation.WEB);
			jom.loadOntology("http://example.org/myOntology#", LoadingLocation.NONE);
		} catch (FileNotFoundException e) {
			logger.debug("Ontology not found, this is what was expected", e);
		} catch (Exception e) {
			logger.error("Unexpected failure", e);
			fail("Unexpected failure");
		}
	}

	/**
	 * Load an ontology from a URL that doesn't exist
	 */
	@Test
	public void testLoadOntologyFromWebLoadingError() {
		logger.info("Running test {}", name.getMethodName());
		try {
			jom.loadOntology("http://example.org/myOntology#", LoadingLocation.NONE);
		} catch (FileNotFoundException e) {
			logger.debug("Ontology not found, this is what was expected", e);
		} catch (Exception e) {
			logger.error("Unexpected failure", e);
			fail("Unexpected failure");
		}
	}

	// Other tests
	/**
	 * Clear the ontology manager
	 */
	@Test
	public void testClear() {
		logger.info("Running test {}", name.getMethodName());
		boolean success;
		try {
			jom.loadOntology("http://xmlns.com/foaf/0.1/", LoadingLocation.NONE);
			jom.clear();
			//semantic factory's prefix map contains 10 standard prefixes, locationmap should be empty
			success = !(jom.getPrefixURIMap().size()>10 || !jom.getImportLocationMap().isEmpty());
		} catch (Exception e) {
			logger.error("Caught exception while clearing store", e);
			success = false;
		}
		if (!success) {
			logger.error("Error clearing JenaOntologyManager");
			fail("Error clearing JenaOntologyManager");
		}
	}

	// TODO: implement more unit tests
}
