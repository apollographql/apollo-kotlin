import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

fun Project.configureNode() {
  check(this == rootProject) {
    "Must only be called in root project"
  }
  plugins.withType(NodeJsRootPlugin::class.java).configureEach {
    extensions.getByType(NodeJsRootExtension::class.java).apply {
//      /**
//       * See https://youtrack.jetbrains.com/issue/KT-63014
//       */
//      version = "21.0.0-v8-canary202309143a48826a08"
//      downloadBaseUrl = "https://nodejs.org/download/v8-canary"
    }

    tasks.withType(KotlinNpmInstallTask::class.java).configureEach {
      args.add("--ignore-engines")
    }
  }

  tasks.withType(KotlinNpmInstallTask::class.java).configureEach {
    args.addAll(
        listOf(
            "--network-concurrency",
            "1",
            "--mutex",
            "network"
        )
    )
  }

  plugins.withType(YarnPlugin::class.java).configureEach {
    project.extensions.getByType(YarnRootExtension::class.java).apply {
      // Drop the patch version because there shouldn't be any dependency change
      lockFileDirectory = projectDir.resolve("kotlin-js-store-${project.getKotlinPluginVersion().substringBeforeLast(".")}")
    }
  }
}
