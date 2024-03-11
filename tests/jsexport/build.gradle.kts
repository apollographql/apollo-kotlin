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
        implementation(libs.apollo.websocket.network.transport.incubating)
      }
    }

    getByName("commonTest") {
      dependencies {
        implementation(libs.apollo.testingsupport)
        implementation(libs.apollo.mockserver)
        implementation(libs.turbine)
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
