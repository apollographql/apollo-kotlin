plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo3")
}

apolloTest()

kotlin {
  sourceSets {
    val nonJsTest = create("nonJsTest") {
      dependsOn(getByName("commonTest"))
    }

    getByName("appleTest").dependsOn(nonJsTest)
    getByName("jvmTest").dependsOn(nonJsTest)

    findByName("commonMain")?.apply {
      dependencies {
        implementation(libs.apollo.runtime)
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
    languageVersion.set("1.5")
  }
}
