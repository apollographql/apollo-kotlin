import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  repositories {
    maven {
      url = uri("../../../../build/localMaven")
    }
    mavenCentral()
  }
  dependencies {
    classpath("com.apollographql.apollo3:apollo-gradle-plugin:APOLLO_VERSION")
    // This project is run with Gradle 5.4 and Kotlin 1.5 doesn't support that so stick with 1.4.32
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.32")
  }
}

apply(plugin = "java")
apply(plugin = "com.apollographql.apollo3")

repositories {
  maven {
    url = uri("../../../../build/localMaven")
  }
  mavenCentral()
}

dependencies {
  add("implementation", "com.apollographql.apollo3:apollo-gradle-plugin")
}

configure<ApolloExtension> {
  packageName.set("com.example")
}

