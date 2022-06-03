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
  java {
    // Override the default toolchain
    @Suppress("UnstableApiUsage")
    toolchain.languageVersion.set(JavaLanguageVersion.of(9))
  }
}
apollo {
  packageName.set("com.example")
}
