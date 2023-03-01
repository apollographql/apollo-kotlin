
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
  id("maven-publish")
}

group = "com.jvm"
version = "1.0.0"

dependencies {
  add("implementation", libs.apollo.api)
}

apollo {
  service("jvm") {
    packageName.set("com.jvm")
    generateApolloMetadata.set(true)
    alwaysGenerateTypesMatching.add(".*")
  }
  service("jvm2") {
    packageName.set("com.jvm2")
    generateApolloMetadata.set(true)
    alwaysGenerateTypesMatching.add(".*")
  }
}

configure<PublishingExtension> {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
    }
    create<MavenPublication>("apollo") {
      from(components["apollo"])
      artifactId = "jvm-producer-apollo"
    }
  }
  repositories {
    maven {
      name = "pluginTest"
      url = uri("file://${rootProject.rootDir.parentFile}/localMaven")
    }
  }
}
