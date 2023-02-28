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

// See https://youtrack.jetbrains.com/issue/KT-56019
val myAttribute = Attribute.of("com.apollographql.test", String::class.java)

configurations.named("releaseFrameworkIosFat").configure {
    attributes {
        attribute(myAttribute, "release-all")
    }
}

configurations.named("debugFrameworkIosFat").configure {
    attributes {
        attribute(myAttribute, "debug-all")
    }
}