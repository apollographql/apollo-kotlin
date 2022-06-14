# Jcenter
./gradlew publishAllPublicationsToBintrayRepository
./gradlew bintrayPublish

# Gradle Plugin Portal
./gradlew :apollo-gradle-plugin:publishPlugins -Pgradle.publish.key="$GRADLE_PUBLISH_KEY" -Pgradle.publish.secret="$GRADLE_PUBLISH_SECRET"

# MavenCentral (go to https://s01.oss.sonatype.org/ to close and release the repository after this is run)
./gradlew publishAllPublicationsToOssStagingRepository
./gradlew sonatypeCloseAndReleaseRepository