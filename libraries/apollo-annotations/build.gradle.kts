plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.annotations"
)

kotlin {
  sourceSets {
    /**
     * Because apollo-annotation is pulled as an API dependency in in all other modules we configure the
     * Kotlin stdlib dependency here. See https://youtrack.jetbrains.com/issue/KT-53471
     */
    findByName("commonMain")?.apply {
      dependencies {
        api(libs.kotlin.stdlib.common)
      }
    }
    findByName("jvmMain")?.apply {
      dependencies {
        api(libs.kotlin.stdlib.jvm)
      }
    }
    findByName("jsMain")?.apply {
      dependencies {
        api(libs.kotlin.stdlib.js)
      }
    }
    findByName("wasmJsMain")!!.apply {
      dependencies {
        api(libs.kotlin.stdlib.wasm.js)
      }
    }
  }
}
