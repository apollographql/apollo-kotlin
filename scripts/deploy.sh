#!/bin/bash
#
# Deploy a jar, source jar, and javadoc jar to Sonatype and/or Bintray

./gradlew publishToBintrayOrOssIfRequired \
-PSONATYPE_NEXUS_USERNAME="${SONATYPE_NEXUS_USERNAME}" -PSONATYPE_NEXUS_PASSWORD="${SONATYPE_NEXUS_PASSWORD}" \
-Pbintray.user="${BINTRAY_USER}" -Pbintray.apikey="${BINTRAY_API_KEY}"

