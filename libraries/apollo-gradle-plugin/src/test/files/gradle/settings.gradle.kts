rootProject.name="testProject"

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../../../../gradle/libraries.toml"))
    }
  }
}
pluginManagement {
  repositories {
    maven {
      url = uri("../../../../build/localMaven")
    }
    gradlePluginPortal()
  }
}
