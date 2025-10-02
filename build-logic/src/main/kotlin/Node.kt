import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask

fun Project.configureNode() {
  check(this == rootProject) {
    "Must only be called in root project"
  }
  plugins.withType(NodeJsRootPlugin::class.java).configureEach {
    tasks.withType(KotlinNpmInstallTask::class.java).configureEach {
      it.args.add("--ignore-engines")
    }
  }

  tasks.withType(KotlinNpmInstallTask::class.java).configureEach {
    it.args.addAll(
        listOf(
            "--network-concurrency",
            "1",
            "--mutex",
            "network"
        )
    )
  }
}
