import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.konan.file.File

object MetalavaHelper {
  fun Project.configureMetalava(downloadMetalavaJar: TaskProvider<DownloadFileTask>) {
    val tmpApiTxt = layout.buildDirectory.file("metalava/api.txt").get().asFile.absolutePath
    val apiTxt = "api.txt"
    registerMetalavaApiTxtTask("generateMetalava", apiTxt, downloadMetalavaJar)
    val generateTmpMetalavaApi = registerMetalavaApiTxtTask("generateTmpMetalavaApi", tmpApiTxt, downloadMetalavaJar)

    tasks.register("checkMetalava", JavaExec::class.java) {
      it.dependsOn(generateTmpMetalavaApi)
      it.classpath(downloadMetalavaJar.flatMap { it.output.asFile })

      val args = listOf("--no-banner",
          "--source-files", tmpApiTxt,
          "--check-compatibility:api:current", apiTxt
      )

      it.setArgs(args)
    }
  }

  fun Project.registerMetalavaApiTxtTask(name: String, apiTxt: String, downloadMetalavaJar: TaskProvider<DownloadFileTask>): TaskProvider<JavaExec> {
    return tasks.register(name, JavaExec::class.java) {
      it.classpath(downloadMetalavaJar.flatMap { it.output.asFile })

      val sources = file("src").walk()
          .maxDepth(2)
          .onEnter { !it.name.toLowerCase().contains("test") }
          .filter {
            it.isDirectory
                && (it.name == "java" || it.name == "kotlin")
          }
          .toList()

      val hides = sources.flatMap {
        it.walk().filter { it.isDirectory && it.name == "internal" }.toList()
      }.map {
        it.relativeTo(projectDir).path.split(File.separator).drop(3).joinToString(".")
      }.distinct()

      val sourcePaths = listOf("--source-path") + sources.joinToString(":")
      val hidePackages = hides.flatMap { listOf("--hide-package", it) }

      val args = listOf("--no-banner", "--api", apiTxt) + sourcePaths + hidePackages

      it.setIgnoreExitValue(true)
      it.setArgs(args)
    }
  }
}