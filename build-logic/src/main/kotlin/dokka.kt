import org.gradle.api.Project
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask

fun Project.maybeCustomizeDokka(moduleName: String? = null) {
  val dokka = extensions.findByType(DokkaExtension::class.java)
  if (dokka == null) {
    return
  }
  dokka.apply {
    pluginsConfiguration.getByName("html") {
      it as DokkaHtmlPluginParameters
      it.customStyleSheets.from(
          listOf("style.css", "prism.css", "logo-styles.css").map { project.rootDir.resolve("dokka/$it") }
      )
      it.customAssets.from(
          listOf("apollo.svg").map { project.rootDir.resolve("dokka/$it") }
      )
    }
  }

  dokka.dokkaSourceSets.configureEach {
    it.includes.from("README.md")
  }
  if (moduleName != null) {
    tasks.withType(DokkaGenerateTask::class.java).configureEach {
      it.generator.moduleName.set(moduleName)
    }
  }
}
