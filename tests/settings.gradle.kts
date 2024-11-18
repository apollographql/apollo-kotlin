pluginManagement {
  includeBuild("../build-logic")
}

plugins {
  id("com.gradle.develocity") version "3.18.2" // sync with libraries.toml
  id("com.gradle.common-custom-user-data-gradle-plugin") version "2.0.2"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
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
      // Do no create intermediate projects as they will fail for apolloTestAgreggation
      // See https://stackoverflow.com/questions/21015353/gradle-intermediate-dir-of-multiproject-not-subproject
      val project = it.relativeTo(rootProject.projectDir).path.replace(File.separatorChar, '-')
      include(project)
      project(":$project").projectDir = it
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
