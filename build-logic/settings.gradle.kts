plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "build-logic"

apply(from = "../gradle/repositories.gradle.kts")
