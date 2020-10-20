import com.apollographql.apollo.gradle.api.ApolloExtension

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
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
  }
}


apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo")

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

configure<ApolloExtension> {
  createAllKotlinJvmSourceSetServices("example") {
    schemaFile.set(file("src/main/graphql/com/example/schema.sdl"))
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    jvmTarget = "1.8"
  }
}