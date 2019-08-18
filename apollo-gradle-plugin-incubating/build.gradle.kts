import groovy.lang.GString

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm")
  id("java-gradle-plugin")
}

fun dep(key: String): String {
  val m = extra["dep"] as Map<*, *>

  val v = m[key]

  // groovy strings with single quotes are GString.
  // groovy strings with double quotes are java.lang.String
  return (v as? String) ?: (v as GString).toString()
}

repositories {
  maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
}

configurations {
  create("testProjects")
}

dependencies {
  compileOnly(gradleApi())

  implementation(dep("kotlinGradlePlugin"))
  implementation(dep("androidPlugin"))
  implementation(project(":apollo-compiler"))
  implementation(dep("kotlinStdLib"))
  
  testImplementation(dep("junit"))
  testImplementation("org.gradle:gradle-tooling-api:5.5.1")

  add("testProjects", dep("androidPlugin"))
  add("testProjects", dep("kotlinGradlePlugin"))
}

// Inspired by: https://github.com/square/sqldelight/blob/83145b28cbdd949e98e87819299638074bd21147/sqldelight-gradle-plugin/build.gradle#L18
// Append any extra dependencies to the test fixtures via a custom configuration classpath. This
// allows us to apply additional plugins in a fixture while still leveraging dependency resolution
// and de-duplication semantics.
tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
  pluginClasspath.from(configurations.getByName("testProjects"))
}

tasks.named<Task>("test") {
  dependsOn(":apollo-api:installLocally")
}

apply(rootProject.file("gradle/gradle-mvn-push.gradle"))
apply(rootProject.file("gradle/bintray.gradle"))
