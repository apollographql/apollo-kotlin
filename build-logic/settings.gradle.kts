plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

rootProject.name = "build-logic"

apply(from = "../gradle/repositories.gradle.kts")
