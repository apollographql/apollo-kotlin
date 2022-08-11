pluginManagement {
  repositories {
    maven {
      url = uri("../../../build/localMaven")
    }
    mavenCentral()
  }
}

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../../../gradle/libs.versions.toml"))
    }
  }
}
