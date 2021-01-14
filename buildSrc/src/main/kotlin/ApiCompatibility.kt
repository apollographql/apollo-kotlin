import JapiCmp.configureJapiCmp
import me.tylerbwong.gradle.metalava.plugin.MetalavaPlugin
import org.gradle.api.Project

object ApiCompatibility {
  fun configure(project: Project) {
    project.subprojects {
      when(it.name) {
        "apollo-compiler" -> {
          // apollo-compiler is for now considered an internal artifact consumed by the Gradle plugin so we allow API changes there.
          return@subprojects
        }
        "apollo-runtime-kotlin" -> {
          // apollo-runtime-kotlin is still under development. Include the check once it is stable enough.
          return@subprojects
        }
        "apollo-normalized-cache" -> {
          // is still under development. Include the check once it is stable enough.
          return@subprojects
        }
        else -> {
          it.configureJapiCmp()
          MetalavaPlugin().apply(it)

        }
      }
    }
  }
}





