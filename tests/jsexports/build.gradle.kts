import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

apolloTest {
  mpp {}
}

kotlin {
  sourceSets {
    val nonJsTest = create("nonJsTest")

    findByName("commonMain")?.apply {
      dependencies {
        implementation(golatac.lib("apollo.runtime"))
      }
    }

    findByName("jvmTest")!!.dependsOn(nonJsTest)
    findByName("appleTest")!!.dependsOn(nonJsTest)

    all {
      languageSettings.optIn("kotlin.js.ExperimentalJsExport")
    }
  }
}

apollo {
  service("service") {
    packageName.set("jsexport")
    jsExport.set(true)
    codegenModels.set("responseBased")
  }
}
