import groovy.lang.GString

buildscript {
  apply("../../../../gradle/dependencies.gradle")
}
plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

repositories {
  maven {
    url = uri("file://${projectDir.absolutePath}/../../../../build/localMaven")
  }
  jcenter()
}

apollo {
  generateKotlinModels = true
}

fun dep(key: String): String {
  val m = extra["dep"] as Map<*, *>

  val v = m[key]

  // groovy strings with single quotes are GString.
  // groovy strings with double quotes are java.lang.String
  return (v as? String) ?: (v as GString).toString()
}

dependencies {
  implementation(dep("jetbrainsAnnotations"))
  implementation(dep("apolloApi"))
  implementation(dep("kotlinStdLib"))
}