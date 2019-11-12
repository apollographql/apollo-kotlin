import com.android.build.gradle.BaseExtension
apply(plugin = "com.android.application")
apply(plugin = "com.apollographql.apollo")
apply(plugin = "kotlin-android")
apply(plugin = "kotlin-android-extensions")

extensions.findByType(BaseExtension::class.java)!!.apply {
    compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())
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

dependencies {
    add("implementation", "com.apollographql.apollo:apollo-android-support")
    add("implementation", "com.apollographql.apollo:apollo-rx2-support")
    add("implementation", "com.apollographql.apollo:apollo-coroutines-support")
    add("implementation", "com.apollographql.apollo:apollo-runtime")
    add("implementation", groovy.util.Eval.x(project, "x.dep.android.appcompat"))
    add("implementation", groovy.util.Eval.x(project, "x.dep.android.recyclerView"))
    add("implementation", groovy.util.Eval.x(project, "x.dep.kotlin.coroutines.android"))
    add("implementation", groovy.util.Eval.x(project, "x.dep.kotlin.coroutines.core"))
    add("implementation", groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))
    add("implementation", groovy.util.Eval.x(project, "x.dep.rx.android"))
    add("implementation", groovy.util.Eval.x(project, "x.dep.rx.java"))
}
