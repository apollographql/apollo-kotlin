import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  apply(from = "../../../gradle/dependencies.gradle")

  repositories {
    maven {
      url = uri("../../../build/localMaven")
    }
    google()
    jcenter()
  }
  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.apollo.plugin"))
    // This project is run with Gradle 5.4 and Kotlin 1.5 doesn't support that so stick with 1.4.32
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.32")
  }
}


apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo3")

repositories {
  maven {
    url = uri("../../../build/localMaven")
  }
  google()
  mavenCentral()
  jcenter()
}

dependencies {
  add("implementation", groovy.util.Eval.x(project, "x.dep.apollo.api"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    jvmTarget = "1.8"
  }
}