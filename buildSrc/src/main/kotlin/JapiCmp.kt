import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.Project
import java.io.File

object JapiCmp {
  fun Project.configureJapiCmp() {
    val downloadBaselineJarTaskProvider = tasks.register("downloadBaseLineJar", DownloadFileTask::class.java) {
      val artifact = when  {
        project.pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform") -> "${project.name}-jvm"
        else -> project.name
      }

      val version = project.findProperty("japicmpBaselineVersion") ?: latestVersion()
      val jar = "$artifact-$version.jar"

      it.url.set("https://jcenter.bintray.com/com/apollographql/apollo/$artifact/$version/$jar")
      it.output.set(File(buildDir, "japicmp/cache/$jar"))
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
    return "https://api.bintray.com/packages/apollographql/android/apollo/versions/_latest"
        .let {
          Request.Builder().url(it).build()
        }.let {
          OkHttpClient().newCall(it).execute()
              .body!!.string()
        }.let {
          (Moshi.Builder().build().adapter(Any::class.java).fromJson(it) as Map<String, Any>).get("name") as String
        }
  }
}