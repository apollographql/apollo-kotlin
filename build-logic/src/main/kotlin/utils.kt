import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

fun Project.getCatalogVersion(alias: String): String {
  return extensions.findByType(VersionCatalogsExtension::class.java)!!.named("libs").findVersion(alias).get().displayName
}