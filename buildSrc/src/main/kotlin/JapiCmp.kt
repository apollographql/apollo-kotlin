import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

object JapiCmp {
  abstract class DownloadBaselineJar : DefaultTask() {
    @get:Input
    lateinit var artifact: String

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun taskAction() {
      val version = (project.findProperty("japicmpBaselineVersion") as? String) ?: latestVersion()


      val jar = "$artifact-$version.jar"

      val url = "https://repo1.maven.org/maven2/com/apollographql/apollo3/$artifact/$version/$jar"
      val client = OkHttpClient()
      val request = Request.Builder().get().url(url).build()

      client.newCall(request).execute().body!!.byteStream().use { body ->
        output.asFile.get().outputStream().buffered().use { file ->
          body.copyTo(file)
        }
      }
    }
  }

  fun Project.configureJapiCmp() {
    val downloadBaselineJarTaskProvider = tasks.register("downloadBaseLineJar", DownloadBaselineJar::class.java) {
      val artifact = when {
        project.pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform") -> "${project.name}-jvm"
        else -> project.name
      }

      it.artifact = artifact
      it.output.set(File(buildDir, "japicmp/cache/$artifact"))
    }

    // TODO: Make this lazy
    afterEvaluate {
      val jarTaskName = when {
        project.pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform") -> "jvmJar"
        else -> "jar"
      }
      val jarTask = tasks.findByName(jarTaskName) as? org.gradle.jvm.tasks.Jar
      if (jarTask != null) {
        tasks.register("generateJapicmp", me.champeau.gradle.japicmp.JapicmpTask::class.java) {
          it.dependsOn(downloadBaselineJarTaskProvider)
          it.oldClasspath = files(downloadBaselineJarTaskProvider.get().output.asFile.get())
          it.newClasspath = files(jarTask.archiveFile)
          it.ignoreMissingClasses = true
          it.packageExcludes = listOf("*.internal*")
          it.onlyModified = true
          it.failOnSourceIncompatibility = false
          it.htmlOutputFile = file("$buildDir/reports/japicmp.html")
          it.txtOutputFile = file("$buildDir/reports/japicmp.txt")
        }
        tasks.register("checkJapicmp", me.champeau.gradle.japicmp.JapicmpTask::class.java) {
          it.dependsOn(downloadBaselineJarTaskProvider)
          it.oldClasspath = files(downloadBaselineJarTaskProvider.get().output.asFile.get())
          it.newClasspath = files(jarTask.archiveFile)
          it.ignoreMissingClasses = true
          it.packageExcludes = listOf("*.internal*")
          it.onlyModified = true
          it.failOnSourceIncompatibility = true
          it.htmlOutputFile = file("$buildDir/reports/japicmp.html")
          it.txtOutputFile = file("$buildDir/reports/japicmp.txt")
        }
      }
    }
  }


  private fun latestVersion(): String {
    return "https://repo1.maven.org/maven2/com/apollographql/apollo/apollo-api/maven-metadata.xml"
        .let {
          Request.Builder().url(it).build()
        }.let {
          OkHttpClient().newCall(it).execute()
              .body!!.string()
              .lines()
              .filter {
                it.contains("latest")
              }.first()
              .replace(Regex(".*<latest>(.*)<latest>.*"), "$1")
        }
  }
}