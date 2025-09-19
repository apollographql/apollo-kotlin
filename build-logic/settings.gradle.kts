plugins {
  id("com.gradle.develocity") version "4.2" // sync with libraries.toml
  id("com.gradle.common-custom-user-data-gradle-plugin") version "2.3"
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
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
