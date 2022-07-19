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
      url = uri("../build/localMaven")
    }
    maven {
      url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
  }
}
