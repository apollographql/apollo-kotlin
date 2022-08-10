plugins {
  id("apollo.test.jvm")
}


dependencies {
  implementation(libs.apollo.runtime)
  implementation(projects.multiModuleRoot)
  apolloMetadata(projects.multiModuleRoot)
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  packageName.set("multimodule.child")
  flattenModels.set(false)
}
