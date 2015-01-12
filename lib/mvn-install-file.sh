#!/bin/bash
mvn install:install-file -DgroupId=com.google.gdata -DartifactId=gdata-core -Dversion=1.0 -Dfile=gdata-core-1.0.jar -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -DgroupId=com.google.gdata -DartifactId=gdata-media -Dversion=1.0 -Dfile=gdata-media-1.0.jar -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -DgroupId=com.google.gdata -DartifactId=gdata-photos -Dversion=2.0 -Dfile=gdata-photos-2.0.jar -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -DgroupId=com.google.gdata -DartifactId=gdata-photos-meta -Dversion=2.0 -Dfile=gdata-photos-meta-2.0.jar -Dpackaging=jar -DgeneratePom=true
