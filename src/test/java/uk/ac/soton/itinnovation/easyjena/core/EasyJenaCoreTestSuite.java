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

import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs all unit tests.
 *
 * @author Vegard Engen
 */
@RunWith(Suite.class)
@SuiteClasses({
	JenaOntologyManagerTest.class,
	HttpStoreWrapperTest.class
	//TODO: add other test classes here: semanticfactory and triple
})
public class EasyJenaCoreTestSuite {

	private static final Logger logger = LoggerFactory.getLogger(EasyJenaCoreTestSuite.class);
	private static final String propertiesFile = "config.properties";

	public static void main(String[] args) throws Exception {
		logger.info("Starting EasyJena-core Test Suite");

		Result result = org.junit.runner.JUnitCore.runClasses(
			JenaOntologyManagerTest.class,
			HttpStoreWrapperTest.class
			//TODO: add other test classes here: semanticfactory and triple
		);

		if (processResults(result)) {
			System.exit(0);
		} else {
			System.exit(1);
		}
	}

	public static boolean processResults(Result result) {
		logger.info("");
		if (result.wasSuccessful()) {
			logger.info("EasyJena-core tests completed successfully!");
		} else {
			logger.info("EasyJena-core tests finished, but with failures!");
		}

		logger.info("Run: " + result.getRunCount() + "  Failed: " + result.getFailureCount() + "  Ignored: " + result.getIgnoreCount());
		logger.info("");
		if (result.getFailureCount() > 0) {
			logger.info("Errors:");
			result.getFailures().stream().forEach((failure) -> {
				logger.info(failure.toString());
			});

			return false;
		}
		return true;
	}

	public static String getPropertiesFile() {
		return propertiesFile;
	}
}
