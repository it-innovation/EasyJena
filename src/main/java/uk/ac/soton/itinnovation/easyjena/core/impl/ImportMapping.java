/////////////////////////////////////////////////////////////////////////
//
// (c) University of Southampton IT Innovation Centre, 2015
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
//		Created Date :			21/09/2015
//		Created for Project:		OPTET
//
/////////////////////////////////////////////////////////////////////////

package uk.ac.soton.itinnovation.easyjena.core.impl;

import uk.ac.soton.itinnovation.easyjena.core.impl.JenaOntologyManager.LoadingLocation;

/**
 * This class represents a mapping of a URI to a location.
 * It can optionally contain information on where the location is (i.e. on disk, web or classpath).
 */
public class ImportMapping {

	private final String uri;
	private final String location;
	private final LoadingLocation type;

	/**
	 * Create an import mapping
	 *
	 * @param uri the URI of the ontology to be mapped
	 * @param location the actual location. Can be another URI, an absolute or a relative path on disk
	 *			or a relative path on classpath
	 * @param type optional parameter specifying whether the location is on disk, web or classpath. If not given,
	 *			all possibilities will be exhausted (LoadingLocation.ALL)
	 */
	public ImportMapping(String uri, String location, LoadingLocation type) {
		this.uri = uri;
		this.location = location;
		this.type = type!=null?type:LoadingLocation.ALL;
	}

	/**
	 * Provides a human-readable form of a triple for printing/debugging purposes
	 *
	 * @return the formatted triple
	 */
	@Override
	public String toString() {
		return "<" + uri + "> -> <" + location + "> (" + type + ")";
	}

	//GETTERS/SETTERS//////////////////////////////////////////////////////////////////////////////

	public String getUri() {
		return uri;
	}

	public String getLocation() {
		return location;
	}

	public LoadingLocation getType() {
		return type;
	}

}
