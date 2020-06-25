import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
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
apply(plugin = "com.apollographql.apollo-kotlin")

repositories {
    maven {
        url = uri("../../../build/localMaven")
    }
    jcenter()
    mavenCentral()
}

