import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  apply(from = "../../../gradle/dependencies.gradle")

  repositories {
    maven {
      url = uri("../../../build/localMaven")
    }
    mavenCentral()
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
  mavenCentral()
}

dependencies {
  add("implementation", groovy.util.Eval.x(project, "x.dep.apollo.api"))
}

configure<ApolloExtension> {
  packageName.set("com.example")
}

