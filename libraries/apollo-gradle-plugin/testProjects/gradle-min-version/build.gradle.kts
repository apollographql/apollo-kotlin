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

configure<ApolloExtension> {
  service("service") {
    packageName.set("com.example")
  }
}

