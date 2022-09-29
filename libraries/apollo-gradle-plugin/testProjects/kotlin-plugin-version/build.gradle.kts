import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  repositories {
    maven {
      url = uri("../../../../build/localMaven")
    }
    mavenCentral()
  }
  dependencies {
    classpath(libs.apollo.plugin)
    // This project is run with Gradle 5.4 and Kotlin 1.5 doesn't support that so stick with 1.4.32
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:KOTLIN_VERSION")
  }
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo3")

repositories {
  maven {
    url = uri("../../../../build/localMaven")
  }
  mavenCentral()
}

dependencies {
  add("implementation", libs.apollo.api)
}

configure<ApolloExtension> {
  packageName.set("com.example")
}

