import com.apollographql.apollo3.gradle.api.ApolloExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

buildscript {
    apply(from = "../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "org.jetbrains.kotlin.multiplatform")
apply(plugin = "com.apollographql.apollo3")

repositories {
    maven {
        url = uri("../../../../build/localMaven")
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
                implementation(libs.apollo.api)
            }
        }
    }
}

configure<ApolloExtension> {
    packageNamesFromFilePaths()
}
