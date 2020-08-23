import JapiCmp.configureJapiCmp
import MetalavaHelper.configureMetalava
import org.gradle.api.Project

object ApiCompatibility {
  fun configure(project: Project) {

    val downloadMetalavaJar = project.tasks.register("downloadMetalava", DownloadFileTask::class.java) {
      it.url.set("https://storage.googleapis.com/android-ci/metalava-full-1.3.0-SNAPSHOT.jar")
      it.output.set(project.layout.buildDirectory.file("metalava/metalava.jar"))
    }

    project.subprojects {
      when(it.name) {
        "apollo-compiler" -> {
          // apollo-compiler is for now considered an internal artifact consumed by the Gradle plugin so we allow API changes there.
          return@subprojects
        }
        "apollo-runtime-kotlin" -> {
          // apollo-runtime-kotlin is still under development. Include the check once it is stable enough.
          return@subprojects
        }
        else -> {
          it.configureJapiCmp()
          it.configureMetalava(downloadMetalavaJar)

        }
      }
    }
  }
}





