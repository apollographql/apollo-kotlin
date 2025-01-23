plugins {
  id("com.gradle.develocity") version "3.19.1" // sync with libraries.toml
  id("com.gradle.common-custom-user-data-gradle-plugin") version "2.1"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "build-logic"

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libraries.toml"))
    }
  }
}

apply(from = "../gradle/repositories.gradle.kts")
apply(from = "../gradle/ge.gradle")
