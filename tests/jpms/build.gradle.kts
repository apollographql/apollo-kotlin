plugins {
  java
  application
  id(libs.plugins.apollo.get().toString())
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.rx2)
  implementation(libs.apollo.normalizedCache.sqlite)
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
  packageName.set("com.example")
}
