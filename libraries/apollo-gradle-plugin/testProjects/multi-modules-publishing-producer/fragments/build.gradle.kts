
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
  id("maven-publish")
}

group = "com.fragments"
version = "1.0.0"

dependencies {
  add("api", project(":schema"))
}

apollo {
  service("service1") {
    packageName.set("com.service1")
    generateApolloMetadata.set(true)
    alwaysGenerateTypesMatching.set(emptyList())

    outgoingVariantsConnection {
      addToSoftwareComponent("java")
      generateApolloMetadata.set(true)
    }
  }
  service("service2") {
    packageName.set("com.service2")
    generateApolloMetadata.set(true)
    alwaysGenerateTypesMatching.set(emptyList())

    outgoingVariantsConnection {
      addToSoftwareComponent("java")
      generateApolloMetadata.set(true)
    }
  }
}

dependencies {
  add("apolloService1", project(":schema"))
  add("apolloService2", project(":schema"))
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

