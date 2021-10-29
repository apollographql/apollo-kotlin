apply(from = "../../../gradle/dependencies.gradle")

project.buildscript.repositories {
  maven {
    url = uri("../../../build/localMaven")
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
      // Some projects are in submoodules, use the
      url = uri(File(project.rootDir, "../../../build/localMaven").absolutePath)
    }
    google()
    mavenCentral()
  }
}