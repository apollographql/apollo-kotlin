val apolloDepth = try {
  rootProject.extra.get("apolloDepth")
} catch (e: Exception) {
  "../../../.."
}

project.buildscript.repositories {
  maven {
    url = uri("$apolloDepth/build/localMaven")
  }
  mavenCentral()
  google()
}

project.buildscript.dependencies.apply {
  add("classpath", libs.kotlin.plugin)
  add("classpath", libs.apollo.plugin)
  add("classpath", libs.android.plugin)
}

allprojects {
  repositories {
    maven {
      // Some projects are in submodules, use the rootDir
      url = uri(File(project.rootDir, "$apolloDepth/build/localMaven").absolutePath)
    }
    mavenCentral()
    google()
  }
}
