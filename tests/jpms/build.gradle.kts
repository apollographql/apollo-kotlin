plugins {
  id("java")
  id("application")
  id("com.apollographql.apollo3")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  implementation("com.apollographql.apollo3:apollo-rx2-support")
  implementation("com.apollographql.apollo3:apollo-normalized-cache-sqlite")
  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
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
