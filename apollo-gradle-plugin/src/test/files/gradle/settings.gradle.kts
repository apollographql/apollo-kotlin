rootProject.name="testProject"

apply("../../../gradle/dependencies.gradle")

pluginManagement {
  repositories {
    maven {
      url = uri("../../../build/localMaven")
    }
    gradlePluginPortal()
  }


  resolutionStrategy {
    eachPlugin {
      if (requested.id.id == "org.jetbrains.kotlin.jvm") {
        useModule(groovy.util.Eval.x(extra, "x.dep.kotlinPlugin"))
      }

      if (requested.id.id == "com.apollographql.apollo3") {
        useModule(groovy.util.Eval.x(extra, "x.dep.apollo.plugin"))
      }
    }
  }
}