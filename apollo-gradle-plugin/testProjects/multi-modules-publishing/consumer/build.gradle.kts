import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  rootProject.extra.set("apolloDepth", "../../../..")
  apply(from = "../../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo3")

dependencies {
  add("implementation", groovy.util.Eval.x(project, "x.dep.apolloApi"))
  add("implementation", "com.jvm:jvm-producer:1.0.0")
  add("apolloMetadata", "com.jvm:jvm-producer-apollo:1.0.0")
}

repositories {
  maven {
    name = "pluginTest"
    url = uri("file://${rootProject.rootDir.parentFile}/localMaven")
  }
}

configure<ApolloExtension> {
  /**
   * Both services use the same schema so it's fine to not set it
   */
  service("jvm") {
    packageName.set("com.consumer")
  }
  service("jvm2") {
    packageName.set("com.consumer2")
  }
}

