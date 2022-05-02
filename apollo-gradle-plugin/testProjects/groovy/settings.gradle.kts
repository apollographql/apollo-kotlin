pluginManagement {
  apply(from = "../../../gradle/dependencies.gradle")
  repositories {
    maven {
      url = uri("../../../build/localMaven")
    }
    mavenCentral()
  }
  resolutionStrategy {
    eachPlugin {
      when (requested.id.id) {
        "com.apollographql.apollo3" -> useModule(groovy.util.Eval.x(settings, "x.dep.apollo.plugin"))
        "org.jetbrains.kotlin.jvm" -> useModule(groovy.util.Eval.x(settings, "x.dep.kotlinPlugin"))
      }
    }
  }
}