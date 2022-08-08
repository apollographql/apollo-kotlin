plugins {
  id(libs.plugins.kotlin.jvm.get().toString())
  id(libs.plugins.apollo.get().toString())
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.mockserver)
  implementation(libs.apollo.normalizedCache)
  implementation(libs.apollo.testingSupport)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
}

apollo {
  service("1") {
    sourceFolder.set("1")
    packageName.set("com.example.one")
  }
  service("2") {
    sourceFolder.set("2")
    packageName.set("com.example.two")
  }
}
