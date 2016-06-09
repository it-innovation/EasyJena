# EasyJena-core

## Overview

This library provides tools to manage ontology models in-memory.
It supports basic functionality like loading, saving, reasoning etc. but provides higher-level
methods than Apache Jena itself.

It also has an HTTP connector for client-side access to SPARQL endpoints. For server-side access
to a triple store please use one of the additional EasyJena modules or extend AStoreWrapper in
your own project.

Parts of this software were developed under different projects to support semantic operations.
The main contributions come from:

- OPTET (http://www.optet.eu/)
- REVEAL (http://revealproject.eu/)
- Experimedia (http://www.experimedia.eu/)

## Installation instructions

```
Build with
    mvn clean install
```

To use EasyJena-core, add the following snippet to your pom file:

```
<dependency>
    <groupId>uk.ac.soton.itinnovation.easyjena</groupId>
    <artifactId>EasyJena-core</artifactId>
    <version>whateverVersion</version>
</dependency>
```

## Documentation

Javadoc has been applied throughout the code and can be built if required.
To get started, see the test files in package uk.ac.soton.itinnovation.easyjena.core.test which
show how to use EasyJena.

## Copyright

The source code in this distribution is (c) Copyright University of Southampton
IT Innovation Centre 2014-2015.
Where contributions come from other sources, this is made clear in the code.

## Dependencies and licensing

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

The licenses for EasyJena-core apply for all EasyJena modules.
Please note the imports' licenses for more information about their imports.

- Apache Jena              Apache License Version 2.0
- SPIN API                 Apache License Version 2.0
- slf4j                    MIT license
- logback                  LGPL v2.1 / EPL v1.0
- logback prettier         Apache License, Version 2.0
- JUnit                    Eclipse Public License 1.0
- Apache commons config    Apache License Version 2.0

## Contact

For further information on collaboration, support or alternative licensing, please contact:

- Website: http://www.it-innovation.soton.ac.uk
- Email: info@it-innovation.soton.ac.uk
- Telephone: +44 (0)23 8059 8866

