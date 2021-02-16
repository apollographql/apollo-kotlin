import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())
    defaultConfig {
        applicationId = "com.apollographql.apollo3.kmpsample"
        minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString())
        targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString())
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
    implementation("com.apollographql.apollo3:apollo-api")

    implementation(groovy.util.Eval.x(project, "x.dep.androidx.appcompat"))
    implementation(groovy.util.Eval.x(project, "x.dep.androidx.recyclerView"))
    implementation(groovy.util.Eval.x(project, "x.dep.kotlin.coroutinesAndroid"))
    implementation(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
}
