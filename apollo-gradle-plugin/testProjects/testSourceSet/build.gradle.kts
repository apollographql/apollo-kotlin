import com.apollographql.apollo.gradle.api.ApolloExtension
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
    jcenter()
  }
  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.apollo.plugin"))
  }
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo")

repositories {
  maven {
    url = uri("../../../build/localMaven")
  }
  jcenter()
  mavenCentral()
}

dependencies {
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.apollo.api"))
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = "1.8"
  }
}

configure<ApolloExtension> {
  withOutputDir {
    val kotlinProjectExtension = project.extensions.get("kotlin") as KotlinProjectExtension
    kotlinProjectExtension.sourceSets.getByName("test").kotlin.srcDir(outputDir)
  }
}