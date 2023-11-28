import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

fun Project.configureNode() {
  tasks.withType(org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask::class.java).configureEach {
    args.addAll(
        listOf(
            "--network-concurrency",
            "1",
            "--mutex",
            "network"
        )
    )
  }

  // See https://youtrack.jetbrains.com/issue/KT-47215
  plugins.withType(YarnPlugin::class.java).configureEach {
    project.extensions.getByType(YarnRootExtension::class.java).apply {
      // Drop the patch version because there shouldn't be any dependency change
      lockFileDirectory = projectDir.resolve("kotlin-js-store-${project.getKotlinPluginVersion().substringBeforeLast(".")}")
    }
  }

  // See https://youtrack.jetbrains.com/issue/KT-49774/KJS-Gradle-Errors-during-NPM-dependencies-resolution-in-parallel-build-lead-to-unfriendly-error-messages-like-Projects-must-be#focus=Comments-27-6271456.0-0
  rootProject.plugins.withType(NodeJsRootPlugin::class.java) {
    project.extensions.getByType(NodeJsRootExtension::class.java).nodeVersion = "16.17.0"
  }
}
