import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider

fun Project.getCatalogVersion(alias: String): String {
  return extensions.findByType(VersionCatalogsExtension::class.java)!!.named("libs").findVersion(alias).get().displayName
}

fun Project.getCatalogLib(alias: String): Provider<MinimalExternalModuleDependency> {
  return extensions.findByType(VersionCatalogsExtension::class.java)!!.named("libs").findLibrary(alias).get()
}