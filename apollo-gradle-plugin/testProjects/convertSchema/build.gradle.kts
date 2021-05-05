buildscript {
  apply(from = "../../../gradle/dependencies.gradle")

  repositories {
    maven {
      url = uri("../../../build/localMaven")
    }
    google()
    mavenCentral()
  }
  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.apollo.plugin"))
  }
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo3")

repositories {
  maven {
    url = uri("../../../build/localMaven")
  }
  mavenCentral()
}
