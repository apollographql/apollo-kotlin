import com.apollographql.apollo3.gradle.api.ApolloExtension
import com.android.build.gradle.BaseExtension

buildscript {
  apply(from = "../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "com.android.library")
apply(plugin = "org.jetbrains.kotlin.android")
apply(plugin = "com.apollographql.apollo3")

dependencies {
  add("implementation", groovy.util.Eval.x(project, "x.dep.apolloApi"))
}

configure<BaseExtension> {
  compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

  defaultConfig {
    minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString())
    targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString())
  }

  // This doesn't really make sense for a library project, but still allows to compile flavor source sets
  flavorDimensions("version")
  productFlavors {
    create("demo") {
      versionNameSuffix = "-demo"
    }
    create("full") {
      versionNameSuffix = "-full"
    }
  }
}

configure<ApolloExtension> {
  createAllAndroidVariantServices(".", "example") {
    // Here we set the same schema file for all variants
    schemaFile.set(file("src/main/graphql/com/example/schema.sdl"))
    packageName.set("com.example")
  }
}
