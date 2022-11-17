apply(from = rootDir.resolve("../../../../gradle/repositories.gradle.kts").absolutePath)

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      // we are in something like
      // - libraries/apollo-gradle-plugin/testProjects/$name
      // - libraries/apollo-gradle-plugin/build/testProject
      from(files(rootDir.resolve("../../../../gradle/libraries.toml").absolutePath))
    }
  }
}

pluginManagement {
  repositories {
    maven {
      url = uri(rootDir.resolve("../../../../build/localMaven").absolutePath)
    }
  }
  resolutionStrategy {
    eachPlugin {
      // Workaround for the Kotlin plugin 1.5.0 not publishing the marker
      if(requested.id.id.startsWith("org.jetbrains.kotlin")) {
        useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
      }
    }
  }
}

dependencyResolutionManagement {
  repositories {
    maven {
      url = uri(rootDir.resolve("../../../../build/localMaven").absolutePath)
    }
  }
}
