plugins {
  id("apollo.test.jvm")
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.mockserver)
  implementation(libs.apollo.normalizedcache)
  implementation(libs.apollo.testingsupport)
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
