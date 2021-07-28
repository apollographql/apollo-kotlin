import com.apollographql.apollo3.gradle.api.ApolloExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  apply(from = "../../../gradle/dependencies.gradle")

  repositories {
    maven {
      url = uri("../../../build/localMaven")
    }
    google()
    mavenCentral()
  }
  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.apollo.plugin"))
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
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.apollo.api"))
}

configure<ApolloExtension> {
  outputDirConnection {
    connectToKotlinSourceSet("test")
  }
}