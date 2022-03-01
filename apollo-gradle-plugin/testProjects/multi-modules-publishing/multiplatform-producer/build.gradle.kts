import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  rootProject.extra.set("apolloDepth", "../../../..")
  apply(from = "../../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "org.jetbrains.kotlin.multiplatform")
apply(plugin = "com.apollographql.apollo3")
apply(plugin = "maven-publish")

group = "com.multiplatform"
version = "1.0.0"

configure<KotlinMultiplatformExtension> {
  jvm()

  sourceSets {
    get("commonMain").dependencies {
      implementation(groovy.util.Eval.x(project, "x.dep.apollo.api"))
    }
  }
}

configure<ApolloExtension> {
  service("multiplatform") {
    packageName.set("com.multiplatform")
    generateApolloMetadata.set(true)
  }
}

configure<PublishingExtension> {
  repositories {
    maven {
      name = "pluginTest"
      url = uri("file://${rootProject.rootDir.parentFile}/localMaven")
    }
  }
}