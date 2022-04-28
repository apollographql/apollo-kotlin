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
        if (System.getProperty("idea.sync.active") == null) {
          "kotlinPlugin"
        } else {
          "kotlinPluginDuringIdeaSync"
        }.let {
          useModule(groovy.util.Eval.x(settings, "x.dep.$it"))
        }
      }
    }
  }
}
