import com.apollographql.apollo.gradle.api.ApolloExtension

buildscript {
  apply(from = "../../../gradle/dependencies.gradle")

  repositories {
    jcenter()
    maven {
      url = uri("../../../build/localMaven")
    }
  }
  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.apollo.plugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
  }
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo")

repositories {
  mavenCentral()
  maven {
    url = uri("../../../../build/localMaven")
  }
}


