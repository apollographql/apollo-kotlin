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
    val nonJsTest = create("nonJsTest") {
      dependsOn(getByName("commonTest"))
    }

    getByName("appleTest").dependsOn(nonJsTest)
    getByName("jvmTest").dependsOn(nonJsTest)

    findByName("commonMain")?.apply {
      dependencies {
        implementation(golatac.lib("apollo.runtime"))
      }
    }

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
    mapScalar("Point", "Point", "PointAdapter")
  }
}
