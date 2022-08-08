plugins {
  id(libs.plugins.apollo.get().toString())
  id(libs.plugins.kotlin.jvm.get().toString())
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(projects.multiModuleRoot)
  apolloMetadata(projects.multiModuleRoot)
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  packageNamesFromFilePaths()
  useSchemaPackageNameForFragments.set(true)
}
