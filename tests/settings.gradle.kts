pluginManagement {
  includeBuild("../build-logic")
}

plugins {
  id("com.gradle.enterprise") version "3.14" // sync with libraries.toml
  id("com.gradle.common-custom-user-data-gradle-plugin") version "1.11.1"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.6.0"
}

rootProject.name = "apollo-tests"

// Include all tests
rootProject.projectDir
    .listFiles()!!
    .filter { it.isDirectory }
    .flatMap {
      it.walk()
    }
    .filter { it.isDirectory && File(it, "build.gradle.kts").exists() }
    .forEach {
      val project = it.relativeTo(rootProject.projectDir).path.replace(File.separatorChar, ':')
      include(project)
    }

includeBuild("../")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libraries.toml"))
    }
  }
}

apply(from = "./gradle/repositories.gradle.kts")
apply(from = "./gradle/ge.gradle")
