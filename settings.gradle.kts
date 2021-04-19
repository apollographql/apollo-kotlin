@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

rootProject.name = "apollo-android"

rootProject.projectDir
    .listFiles()
    .filter { it.isDirectory }
    .filter { it.name.startsWith("apollo-") }
    .forEach {
      include(it.name)
    }

includeBuild("build-logic")
