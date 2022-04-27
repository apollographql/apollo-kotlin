import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.configureJavaAndKotlinCompilers() {
  // Because this is called from subproject {}, it might actually be called before the java/kotlin plugins are applied
  // XXX: replace with plugins.withId("") {}
  afterEvaluate {
    // For Kotlin JVM projects
    tasks.withType(KotlinCompile::class.java) {
      it.kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
        if (getKotlinPluginVersion() == "1.6.10") {
          // This is a workaround for https://youtrack.jetbrains.com/issue/KT-47000 (fixed in Kotlin 1.6.20)
          // Since we don't use @JvmDefault anywhere, the option has no effect, but suppresses the bogus compiler error
          // See also https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-default/
          // See also https://blog.jetbrains.com/kotlin/2020/07/kotlin-1-4-m3-generating-default-methods-in-interfaces/
          freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=compatibility"
        }
        apiVersion = "1.5"
        languageVersion = "1.5"
      }
    }

    // For Kotlin Multiplatform projects
    (project.extensions.findByName("kotlin")
        as? org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension)?.run {
      sourceSets.all {
        it.languageSettings.optIn("kotlin.RequiresOptIn")
      }
    }

    // Ensure "org.gradle.jvm.version" is set to "8" in Gradle metadata of jvm-only modules.
    // (multiplatform modules don't set this)
    project.extensions.getByType(JavaPluginExtension::class.java).apply {
      sourceCompatibility = JavaVersion.VERSION_1_8
      targetCompatibility = JavaVersion.VERSION_1_8
    }

    // Android projects target 1.8 starting with 4.2, no need to do anything
    // https://developer.android.com/studio/releases/gradle-plugin#4-2-0
  }
}
