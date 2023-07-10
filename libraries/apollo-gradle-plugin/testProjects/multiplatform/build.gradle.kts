import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.apollo)
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

apollo {
    service("service") {
        packageNamesFromFilePaths()
    }
}
