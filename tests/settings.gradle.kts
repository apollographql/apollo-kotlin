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

includeBuild("../") {
  // See https://youtrack.jetbrains.com/issue/KT-56536
  name = "apollo-kotlin"
}

pluginManagement {
  includeBuild("../build-logic")
}

plugins {
  id("com.gradle.enterprise") version "3.12.4"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

apply(from = "./gradle/repositories.gradle.kts")

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    val isCiBuild = System.getenv("CI") != null
    isUploadInBackground = !isCiBuild
  }
}

