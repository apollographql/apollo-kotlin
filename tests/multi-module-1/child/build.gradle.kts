plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(projects.multiModule1.root)
  apolloMetadata(projects.multiModule1.root)
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  packageName.set("multimodule1.child")
  flattenModels.set(false)
}
