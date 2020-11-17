import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    apply(from = "../../../gradle/dependencies.gradle")

    repositories {
        maven {
            url = uri("../../../build/localMaven")
        }
        mavenCentral()
    }
    dependencies {
        classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
        classpath(groovy.util.Eval.x(project, "x.dep.apollo.plugin"))
    }
}

apply(plugin = "org.jetbrains.kotlin.multiplatform")
apply(plugin = "com.apollographql.apollo")

repositories {
    maven {
        url = uri("../../../build/localMaven")
    }
    mavenCentral()
}

configure<KotlinMultiplatformExtension> {
    iosArm64 {
        binaries {
            framework {
            }
        }
    }
    sourceSets {
        get("commonMain").apply {
            dependencies {
                implementation(groovy.util.Eval.x(project, "x.dep.apollo.api"))
            }
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}
