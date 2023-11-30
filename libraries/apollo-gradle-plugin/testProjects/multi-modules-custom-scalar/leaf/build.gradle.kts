plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
  id("application")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(libs.apollo.api)
  testImplementation(libs.kotlin.test.junit)

  implementation(project(":root"))
}

application {
  mainClass.set("LeafKt")
}

apollo {
  service("service") {
    dependsOn(project(":root"))
    packageName.set("com.library")
  }
}
