plugins {
  id("application")
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.junit)
}

apollo {
  service("service") {
    packageName.set("termination")
  }
}

application {
  mainClass.set("termination.MainKt")
}

tasks.named("build").configure {
  dependsOn("run")
}