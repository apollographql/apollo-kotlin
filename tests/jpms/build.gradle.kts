plugins {
  id("java")
  id("application")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.rx2)
  implementation(libs.apollo.normalizedcache.sqlite)
  testImplementation(libs.junit)
}

application {
  mainModule.set("com.example.app") // name defined in module-info.java
  mainClass.set("com.example.app.Main")
}

afterEvaluate {
  project.tasks.withType(JavaCompile::class.java).configureEach {
    // Override the default. JPMS is only available with Java9+
    options.release.set(9)
  }
}
apollo {
  service("service") {
    packageName.set("com.example")
  }
}
