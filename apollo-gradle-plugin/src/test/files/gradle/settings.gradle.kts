rootProject.name="testProject"

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../../../gradle/libs.versions.toml"))
    }
  }
}
pluginManagement {
  repositories {
    maven {
      url = uri("../../../build/localMaven")
    }
    gradlePluginPortal()
  }
}
