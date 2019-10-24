plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm")
  id("java-gradle-plugin")
}

// groovy strings with double quotes are GString.
// groovy strings with single quotes are java.lang.String
// In all cases, gradle APIs take Any so just feed them whatever is returned
fun dep(key: String) = (extra["dep"] as Map<*, *>)[key]!!

fun Any.dot(key: String): Any {
  return (this as Map<String, *>)[key]!!
}

dependencies {
  compileOnly(gradleApi())
  compileOnly(dep("kotlin").dot("plugin"))
  compileOnly(dep("android").dot("plugin"))

  implementation(project(":apollo-compiler"))
  implementation(dep("kotlin").dot("stdLib"))
  implementation(dep("okHttp").dot("okHttp"))
  implementation(dep("moshi").dot("moshi"))
  
  testImplementation(dep("junit"))
  testImplementation(dep("okHttp").dot("mockWebServer"))
}

tasks.withType<Test> {
  dependsOn(":apollo-api:installLocally")
  dependsOn(":apollo-compiler:installLocally")
  dependsOn("installLocally")

  inputs.dir("src/test/files")
}

apply(rootProject.file("gradle/gradle-mvn-push.gradle"))
apply(rootProject.file("gradle/bintray.gradle"))
