import JapiCmp.configureJapiCmp
import MetalavaHelper.configureMetalava
import org.gradle.api.Project

object ApiCompatibility {
  fun apply(project: Project) {

    val downloadMetalavaJar = project.tasks.register("downloadMetalava", DownloadFileTask::class.java) {
      it.url.set("https://storage.googleapis.com/android-ci/metalava-full-1.3.0-SNAPSHOT.jar")
      it.output.set(project.layout.buildDirectory.file("metalava/metalava.jar"))
    }

    project.subprojects {
      it.configureJapiCmp()
      it.configureMetalava(downloadMetalavaJar)
    }
  }
}





