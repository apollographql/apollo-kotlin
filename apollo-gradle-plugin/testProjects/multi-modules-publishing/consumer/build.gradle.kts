import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  rootProject.extra.set("apolloDepth", "../../../..")
  apply(from = "../../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo3")

dependencies {
  add("implementation", groovy.util.Eval.x(project, "x.dep.apollo.api"))
  add("implementation", "com.jvm:jvm-producer:1.0.0")
  add("apolloMetadata", "com.jvm:jvm-producer:1.0.0")
}

repositories {
  maven {
    name = "pluginTest"
    url = uri("file://${rootProject.rootDir.parentFile}/localMaven")
  }
}

configure<ApolloExtension> {
  packageName.set("com.consumer")
}

