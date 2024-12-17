
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
    dependsOn(project(":schema"))
    outgoingVariantsConnection {
      addToSoftwareComponent("java")
    }
  }
  service("service2") {
    packageName.set("com.service2")
    dependsOn(project(":schema"))
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
