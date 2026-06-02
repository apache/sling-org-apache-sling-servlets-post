[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

&#32;[![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-servlets-post/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-servlets-post/job/master/)&#32;[![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-servlets-post/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-servlets-post/job/master/test/?width=800&height=600)&#32;[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-servlets-post&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-servlets-post)&#32;[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-servlets-post&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-servlets-post)&#32;[![JavaDoc](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.servlets.post.svg)](https://www.javadoc.io/doc/org.apache.sling/org.apache.sling.servlets.post)&#32;[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.servlets.post/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.servlets.post%22)&#32;[![servlets](https://sling.apache.org/badges/group-servlets.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/groups/servlets.md) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling Default POST Servlets

Provides the default POST servlet bundle for Apache Sling.

This module is part of the [Apache Sling](https://sling.apache.org) project. You can read more about this module on our [documentation site](https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html).

## Overview

The bundle provides the default `SlingPostServlet` and built-in POST operations for Sling content changes, including:

- create and modify
- delete
- copy and move
- import
- restore
- checkin/checkout and versioning helpers
- file upload (regular, streamed, and chunked)

The implementation is **Jakarta Servlet-first** and uses Sling Jakarta APIs. Legacy `javax.servlet` SPI integration remains supported via adapters in `impl/wrapper` and service proxying in `PostOperationProxyProvider`.

## Extension points

Custom behavior can be provided via OSGi services such as:

- `JakartaPostOperation`
- `SlingJakartaPostProcessor`
- `JakartaNodeNameGenerator`
- `JakartaPostResponseCreator`

The corresponding legacy extension types (`PostOperation`, `SlingPostProcessor`, `NodeNameGenerator`, `PostResponseCreator`) are still supported through compatibility wrappers.

## Build and test

Java 17 is required.

- Full build: `mvn clean install`
- Build without tests: `mvn clean install -DskipTests`
- Unit tests: `mvn test`
- Integration tests (Failsafe, including `ModifyOperationIT`): `mvn verify`
- Single unit test class: `mvn test -Dtest=HtmlResponseTest`
- Single integration test class: `mvn verify -Dit.test=ModifyOperationIT`
- Inspect generated bundle metadata: `jar tf target/org.apache.sling.servlets.post-*.jar | grep -E 'META-INF/MANIFEST.MF|SLING-INF/nodetypes/chunk.cnd'`

## Manual upload smoke tests

For manual file-upload protocol checks against a running Sling instance on `localhost:8080` (`admin:admin`), use:

`sh developer-tests/testFileUploads.sh <testfile>`

The script uploads using regular, streamed, and chunked streamed protocols, then downloads and compares content. See `developer-tests/README.md` and `Protocols.md` for protocol and script details.

## Repository layout

```text
pom.xml                        Maven build descriptor (packaging: jar)
bnd.bnd                        OSGi bundle instructions and embedded resources
Jenkinsfile                    ASF Jenkins pipeline definition
Protocols.md                   Protocol notes for POST and upload behavior
src/
  main/java/org/apache/sling/servlets/post/
    *.java                     Public Jakarta-first API/SPI (+ legacy compatibility APIs)
    exceptions/                Persistence-related exceptions
    impl/                      Internal servlet and operation implementations
      PostOperationProxyProvider.java  Legacy service proxy registration
      operations/              Built-in POST operations
      helper/                  Internal helpers (upload, property handling, naming, chunking)
      wrapper/                 Jakarta <-> javax bridging adapters
  main/resources/
    SLING-INF/nodetypes/chunk.cnd   Chunked upload node type definitions
    org/apache/sling/servlets/post/ HTML response templates
    system/sling.js            Bundled JS resource
  test/java/                   Unit and integration tests
developer-tests/               Manual developer test scripts
```

## Notes

- OSGi metadata is generated with bnd (`bnd-maven-plugin`), with API baseline checks via `bnd-baseline-maven-plugin`.
- The build shades selected classes from `jackrabbit-jcr-commons` and `sling-jcr-contentparser` into internal `impl` packages.
- JCR (`javax.jcr.*`) and `org.apache.sling.jcr.contentloader` imports are configured as dynamic for runtime flexibility.
- The bundle uses `jakarta.servlet-api` as the primary servlet API while keeping `javax.servlet-api` and `org.apache.felix.http.wrappers` for compatibility adapters.
- JSON support is split between Jakarta JSON APIs for Jakarta responses and legacy JSON support for backwards-compatible APIs.
