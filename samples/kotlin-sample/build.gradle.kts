import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply(plugin = "com.android.application")
apply(plugin = "com.apollographql.apollo")
apply(plugin = "kotlin-android")
apply(plugin = "kotlin-android-extensions")

extensions.findByType(BaseExtension::class.java)!!.apply {
    compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    defaultConfig {
        applicationId = "com.apollographql.apollo.kotlinsample"
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
    add("implementation", "com.apollographql.apollo:apollo-android-support")
    add("implementation", "com.apollographql.apollo:apollo-rx2-support")
    add("implementation", "com.apollographql.apollo:apollo-coroutines-support")
    add("implementation", "com.apollographql.apollo:apollo-runtime")
    add("implementation", "com.apollographql.apollo:apollo-http-cache")
    add("implementation", groovy.util.Eval.x(project, "x.dep.okHttp.logging"))
    add("implementation", groovy.util.Eval.x(project, "x.dep.androidx.appcompat"))
    add("implementation", groovy.util.Eval.x(project, "x.dep.androidx.recyclerView"))
    add("implementation", groovy.util.Eval.x(project, "x.dep.kotlin.coroutinesAndroid"))
    add("implementation", groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
    add("implementation", groovy.util.Eval.x(project, "x.dep.rx.android"))
    add("implementation", groovy.util.Eval.x(project, "x.dep.rx.java"))
}

configure<com.apollographql.apollo.gradle.api.ApolloExtension> {
    generateKotlinModels.set(true)
}
