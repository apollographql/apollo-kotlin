import org.gradle.api.Project

fun Project.configureRepositories() {
  repositories.apply {
    mavenCentral()
    google()
    @Suppress("DEPRECATION")
    jcenter {
      content {
        // https://github.com/Kotlin/kotlinx-nodejs/issues/16
        includeModule("org.jetbrains.kotlinx", "kotlinx-nodejs")
      }
    }
  }
}
