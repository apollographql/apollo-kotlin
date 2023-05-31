plugins {
  kotlin("jvm") version "1.8.10"
  id("com.apollographql.apollo3") version "3.8.1"
  application
}

group = "org.example"
version = "1.0.0-SNAPSHOT"

allprojects {
  repositories {
    mavenLocal()
    google()
    mavenCentral()
  }
}

dependencies {
  implementation("com.apollographql.apollo3", "apollo-runtime")
}

apollo {
  service("main") {
    packageName.set("com.example")
  }
}

application {
  mainClass.set("com.example.MainKt")
}
