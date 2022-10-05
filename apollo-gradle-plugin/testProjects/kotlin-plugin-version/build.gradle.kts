import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  repositories {
    maven {
      url = uri("../../../build/localMaven")
    }
    mavenCentral()
  }
  dependencies {
    classpath(libs.apollo.plugin)
    classpath(libs.kotlin.plugin.min)
  }
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo3")

repositories {
  maven {
    url = uri("../../../build/localMaven")
  }
  mavenCentral()
}

dependencies {
  add("implementation", libs.apollo.api)
}

configure<ApolloExtension> {
  packageName.set("com.example")
}

