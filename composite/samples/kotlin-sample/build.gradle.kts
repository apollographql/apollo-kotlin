import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    id("com.apollographql.apollo3")
    id("kotlin-android")
    id("kotlin-android-extensions")
}

extensions.findByType(BaseExtension::class.java)!!.apply {
    compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    defaultConfig {
        applicationId = "com.apollographql.apollo3.kotlinsample"
        minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString())
        targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString())

        val f = project.file("github_token")
        if (!f.exists()) {
            f.writeText("your_token")
        }

        buildConfigField("String", "GITHUB_OAUTH_TOKEN", "\"${f.readText().trim()}\"")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    implementation("com.apollographql.apollo3:deprecated-apollo-rx2-support")
    implementation("com.apollographql.apollo3:deprecated-apollo-coroutines-support")
    implementation("com.apollographql.apollo3:deprecated-apollo-runtime")
    implementation("com.apollographql.apollo3:deprecated-apollo-http-cache")
    implementation("com.apollographql.apollo3:deprecated-apollo-android-support")
    implementation("com.apollographql.apollo3:apollo-normalized-cache")
    implementation("com.apollographql.apollo3:apollo-normalized-cache-sqlite")
    implementation(groovy.util.Eval.x(project, "x.dep.okHttp.logging"))
    implementation(groovy.util.Eval.x(project, "x.dep.androidx.appcompat"))
    implementation(groovy.util.Eval.x(project, "x.dep.androidx.recyclerView"))
    implementation(groovy.util.Eval.x(project, "x.dep.kotlin.coroutinesAndroid"))
    implementation(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
    implementation(groovy.util.Eval.x(project, "x.dep.rx.android"))
    implementation(groovy.util.Eval.x(project, "x.dep.rx.java"))
}

apollo {
    filePathAwarePackageNameGenerator()
    codegenModels.set("responseBased")
}
