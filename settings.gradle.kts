@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

rootProject.name = "apollo-android"

rootProject.projectDir
    .listFiles()
    .filter { it.isDirectory }
    .filter { it.name.startsWith("apollo-") || it.name.startsWith("deprecated-")}
    .forEach {
      if (System.getProperty("idea.sync.active") != null
          && it.name in listOf("deprecated-apollo-android-support", "deprecated-apollo-idling-resource")) {
        return@forEach
      }
      include(it.name)
    }

includeBuild("build-logic")
