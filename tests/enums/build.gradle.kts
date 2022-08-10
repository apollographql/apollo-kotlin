plugins {
  id("apollo.test.jvm")
}

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
}

apollo {
  packageName.set("enums")
  sealedClassesForEnumsMatching.set(listOf(".*avity", "FooSealed"))
}
