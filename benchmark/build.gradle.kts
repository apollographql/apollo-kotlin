buildscript {
  apply(from = "../gradle/dependencies.gradle")
  repositories {
    mavenCentral()
    google()
    maven {
      url = uri("../build/localMaven")
    }
  }
  dependencies {
    classpath("com.apollographql.apollo3.benchmark:build-logic")
  }
}

allprojects {
  repositories {
    mavenCentral()
    google()
    maven {
      url = rootProject.uri("../build/localMaven")
    }
  }
}
