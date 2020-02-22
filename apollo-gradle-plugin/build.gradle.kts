plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm")
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish") version "0.10.1"
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

  api(project(":apollo-compiler"))
  implementation(dep("kotlin").dot("stdLib"))
  implementation(dep("okHttp").dot("okHttp"))
  implementation(dep("moshi").dot("moshi"))
  
  testImplementation(dep("junit"))
  testImplementation(dep("okHttp").dot("mockWebServer"))
}

tasks.withType<Test> {
  // Restart the daemon once in a while or we end up running out of MetaSpace
  // It's not clear if it's a real ClassLoader leak or something else. The okio timeout thread does hold some ClassLoaders
  // for up to 60s. The heap dumps also show some process reaper threads but it might just as well be a temporary thing, not sure.
  // See https://github.com/gradle/gradle/issues/8354
  setForkEvery(8L)
  dependsOn(":apollo-api:publishDefaultPublicationToPluginTestRepository")
  dependsOn(":apollo-compiler:publishDefaultPublicationToPluginTestRepository")
  dependsOn("publishDefaultPublicationToPluginTestRepository")

  inputs.dir("src/test/files")
}

pluginBundle {
  website = "https://github.com/apollographql/apollo-android"
  vcsUrl = "https://github.com/apollographql/apollo-android"
  tags = listOf("graphql", "apollo", "apollographql", "kotlin", "java", "jvm", "android", "graphql-client")
}

gradlePlugin {
  plugins {
    create("apolloGradlePlugin") {
      id = "com.apollographql.apollo"
      displayName = "Apollo-Android GraphQL client plugin."
      description = "Automatically generates typesafe java and kotlin models from your GraphQL files."
      implementationClass = "com.apollographql.apollo.gradle.internal.ApolloPlugin"
    }
  }
}
