plugins {
  id("java")
  id("application")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
  implementation(golatac.lib("apollo.rx2"))
  implementation(golatac.lib("apollo.normalizedcache.sqlite"))
  testImplementation(golatac.lib("junit"))
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
