import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())
    defaultConfig {
        applicationId = "com.apollographql.apollo.kmpsample"
        minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString())
        targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString())

        val f = project.file("github_token")
        if (!f.exists()) {
            f.writeText("your_token")
        }

        buildConfigField("String", "GITHUB_OAUTH_TOKEN", "\"${f.readText().trim()}\"")
    }

    viewBinding {
        isEnabled = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packagingOptions {
        exclude("META-INF/main.kotlin_module")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    implementation(project(":multiplatform:kmp-lib-sample"))

    implementation("com.apollographql.apollo:apollo-coroutines-support")
    implementation("com.apollographql.apollo:apollo-runtime")
    implementation(groovy.util.Eval.x(project, "x.dep.androidx.appcompat"))
    implementation(groovy.util.Eval.x(project, "x.dep.androidx.recyclerView"))
    implementation(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines.android"))
    implementation(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines.core"))
    implementation(groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))
}
