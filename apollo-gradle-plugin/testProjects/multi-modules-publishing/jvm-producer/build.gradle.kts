import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  rootProject.extra.set("apolloDepth", "../../../..")
  apply(from = "../../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo3")
apply(plugin = "maven-publish")

group = "com.jvm"
version = "1.0.0"

dependencies {
  add("implementation", groovy.util.Eval.x(project, "x.dep.apolloApi"))
}

configure<ApolloExtension> {
  service("jvm") {
    packageName.set("com.jvm")
    generateApolloMetadata.set(true)
  }
  service("jvm2") {
    packageName.set("com.jvm2")
    generateApolloMetadata.set(true)
  }
}

configure<PublishingExtension> {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
    }
  }
  repositories {
    maven {
      name = "pluginTest"
      url = uri("file://${rootProject.rootDir.parentFile}/localMaven")
    }
  }
}