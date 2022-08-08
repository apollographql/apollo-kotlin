plugins {
  id(libs.plugins.kotlin.jvm.get().toString())
  id(libs.plugins.apollo.get().toString())
}

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
}

apollo {
  packageName.set("reserved")
}
