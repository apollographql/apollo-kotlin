import com.apollographql.apollo.gradle.api.ApolloExtension

buildscript {
  repositories {
    maven {
      url = uri("../../../../build/localMaven")
    }
    mavenCentral()
  }
  dependencies {
    classpath("com.apollographql.apollo:apollo-gradle-plugin:APOLLO_VERSION")
  }
}



apply(plugin = "java")
apply(plugin = "com.apollographql.apollo")

repositories {
  maven {
    url = uri("../../../../build/localMaven")
  }
  mavenCentral()
  // Uncomment this one to use the Kotlin "dev" repository
  maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/") }
}

configure<ApolloExtension> {
  service("service") {
    packageName.set("com.example")
  }
}

