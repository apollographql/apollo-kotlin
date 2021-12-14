rootProject.name = "build-logic"

apply(from = "../gradle/dependencies.gradle")

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id.startsWith("org.jetbrains.kotlin.jvm")) {
        useModule(groovy.util.Eval.x(settings, "x.dep.kotlinPlugin"))
      }
    }
  }
}
