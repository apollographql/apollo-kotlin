import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.runtime",
    androidOptions = AndroidOptions(withCompose = false)
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-mpp-utils"))
        implementation(libs.atomicfu.library) // https://github.com/Kotlin/kotlinx.coroutines/issues/3968
        api(libs.okio)
        api(libs.uuid)
        api(libs.kotlinx.coroutines)
      }
    }

    fun KotlinDependencyHandler.commonTestDependencies() {
      implementation(libs.apollo.mockserver)
      implementation(libs.turbine)
      implementation(project(":apollo-testing-support-internal")) {
        because("runTest")
        // We have a circular dependency here that creates a warning in JS
        // w: duplicate library name: com.apollographql.apollo:apollo-mockserver
        // See https://youtrack.jetbrains.com/issue/KT-51110
        // We should probably remove this circular dependency but for the time being, just use excludes
        exclude(group = "com.apollographql.apollo", module = "apollo-runtime")
      }
    }
    findByName("commonTest")?.apply {
      dependencies {
        commonTestDependencies()
      }
    }

    findByName("androidInstrumentedTest")?.apply {
      dependencies {
        commonTestDependencies()
      }
    }

    findByName("jvmCommonMain")?.apply {
      dependencies {
        api(libs.okhttp)
      }
    }

    findByName("androidMain")?.apply {
      dependencies {
        implementation(libs.androidx.annotation)
        implementation(libs.androidx.core)
      }
    }

    findByName("jsMain")?.apply {
      dependencies {
        implementation(libs.ktor.client.js.get().toString()) {
          because("We use in the ktor client in DefaultWebSocketEngine")
        }
      }
    }

    findByName("wasmJsMain")?.apply {
      dependencies {
        implementation(libs.kotlinx.browser)
      }
    }

    findByName("appleMain")?.apply {
      dependencies {
      }
    }

    findByName("linuxMain")?.apply {
      dependencies {
        implementation(libs.ktor.client.curl)
      }
    }

    findByName("jvmTest")?.apply {
      dependencies {
        implementation(libs.kotlin.test.junit)
        implementation(libs.truth)
        implementation(libs.okhttp)
      }
    }
  }
}
