
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
  id("maven-publish")
}

group = "com.schema"
version = "1.0.0"

dependencies {
  add("api", libs.apollo.api)
}

apollo {
  service("service1") {
    packageName.set("com.service1")
    generateApolloMetadata.set(true)
    alwaysGenerateTypesMatching.add(".*")
    outgoingVariantsConnection {
      addToSoftwareComponent("java")
    }
  }
  service("service2") {
    packageName.set("com.service2")
    generateApolloMetadata.set(true)
    alwaysGenerateTypesMatching.add(".*")
    outgoingVariantsConnection {
      addToSoftwareComponent("java")
    }
  }
}

configure<PublishingExtension> {
  publications {
    create<MavenPublication>("default") {
      from(components["java"])
    }
  }
  repositories {
    maven {
      name = "pluginTest"
      url = uri("file://${rootProject.rootDir.parentFile}/localMaven")
    }
  }
}
