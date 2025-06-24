#! /bin/bash

mvn -o install

rm $JBOSS422/server/default/deploy/jbpm/jbpm-integration.beans/jbpm-integration-spec*.jar
cp target/jbpm-integration-spec-3.3.1-SNAPSHOT.jar $JBOSS422/server/default/deploy/jbpm/jbpm-integration.beans
