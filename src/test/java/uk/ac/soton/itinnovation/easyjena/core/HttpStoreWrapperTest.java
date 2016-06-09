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

import java.io.IOException;
import java.util.Properties;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.soton.itinnovation.easyjena.core.impl.HttpStoreWrapper;

@RunWith(JUnit4.class)
public class HttpStoreWrapperTest extends TestCase
{
    private HttpStoreWrapper store;
    private static Logger logger;
	private static Properties props;

    @BeforeClass
    public static void beforeClass() {
		logger = LoggerFactory.getLogger(HttpStoreWrapperTest.class);
		props = getProperties();
        logger.info("JenaOntologyManager tests executing...");
    }

    @Before
    public void beforeEachTest() {
		try {
			store = new HttpStoreWrapper(props);
		} catch (Exception e) {
			logger.error("Error creating HttpStoreWrapper prior to test", e);
		}
    }

	@After
	public void afterEachTest() {
		try {
			if (store !=null) {
				store = null;
			}
		} catch (Exception e) {
			logger.error("Error destroying HttpStoreWrapper", e);
		}
	}

    public static Properties getProperties() {
        props = new Properties();
        try {
            props.load(HttpStoreWrapperTest.class.getClassLoader().getResourceAsStream("easyjena.properties"));
        } catch (IOException ex) {
            logger.error("Error with loading properties file", ex);
            return null;
        }
        return props;
    }

	// Tests //////////////////////////////////////////////////////////////////////////////////////

	@Test
	public void testConnect() {
		try {
			store.connect();
		} catch (Exception e) {
			logger.error("Error connecting to store {}", store, e);
			fail("Error connecting to store");
		}
	}

	// not testing graphExists for HTTPStoreWrapper as support is patchy at best

	@Test
	public void testCreateGraph() {
		boolean passed = false;
		try {
			if (store.graphExists("Testrepo-7485738475347584375")) {
				store.deleteGraph("Testrepo-7485738475347584375");
			}
			if (!store.graphExists("Testrepo-7485738475347584375")) {
				store.createGraph("Testrepo-7485738475347584375");
			}
			//should not exist now
			if (store.graphExists("Testrepo-7485738475347584375")) {
				passed = true;
			}
		} catch (Throwable e) {
			logger.error("Error creating graph for store {}", store, e);
		}
		if (!passed) {
			fail("Error creating graph");
		}
	}
	
}
