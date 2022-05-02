val apolloDepth = try {
  rootProject.extra.get("apolloDepth")
} catch (e: Exception) {
  "../../.."
}

apply(from = "$apolloDepth/gradle/dependencies.gradle")

project.buildscript.repositories {
  maven {
    url = uri("$apolloDepth/build/localMaven")
  }
  mavenCentral()
  google()
}

project.buildscript.dependencies.apply {
  add("classpath", groovy.util.Eval.x(project, "x.dep.kotlinPlugin"))
  add("classpath", groovy.util.Eval.x(project, "x.dep.apollo.plugin"))
  add("classpath", groovy.util.Eval.x(project, "x.dep.android.plugin"))
}

allprojects {
  repositories {
    maven {
      // Some projects are in submodules, use the rootDir
      url = uri(File(project.rootDir, "$apolloDepth/build/localMaven").absolutePath)
    }
    google()
    mavenCentral()
  }
}