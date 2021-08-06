import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
    apply(from = "../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "org.jetbrains.kotlin.multiplatform")
apply(plugin = "com.apollographql.apollo3")

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

configure<ApolloExtension> {
    packageNamesFromFilePaths()
}
