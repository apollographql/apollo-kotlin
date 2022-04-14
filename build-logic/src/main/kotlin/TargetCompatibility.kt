import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.configureJavaAndKotlinCompilers() {
  // Because this is called from subproject {}, it might actually be called before the java/kotlin plugins are applied
  // XXX: replace with plugins.withId("") {}
  afterEvaluate {
    // For Kotlin JVM projects
    tasks.withType(KotlinCompile::class.java) {
      it.kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
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
