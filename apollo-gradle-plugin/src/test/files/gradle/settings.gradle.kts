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
      if (requested.id.id == "com.apollographql.apollo3") {
        useModule(groovy.util.Eval.x(extra, "x.dep.apolloPlugin"))
      }
    }
  }
}
