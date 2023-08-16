plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.mockserver")
  mpp {
    withLinux.set(false)
  }
}

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-annotations"))
        api(libs.okio)
        implementation(libs.atomicfu.get().toString()) {
          because("We need locks for native (we don't use the gradle plugin rewrite)")
        }
        api(libs.kotlinx.coroutines)
      }
    }

    findByName("jsMain")?.apply {
      dependencies {
        implementation(libs.kotlinx.nodejs)
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(project(":apollo-testing-support")) {
          because("runTest")
          // We have a circular dependency here that creates a warning in JS
          // w: duplicate library name: com.apollographql.apollo3:apollo-mockserver
          // See https://youtrack.jetbrains.com/issue/KT-51110
          // We should probably remove this circular dependency but for the time being, just use excludes
          exclude(group = "com.apollographql.apollo3", module = "apollo-mockserver")
        }
        implementation(project(":apollo-runtime")) {
          because("We need HttpEngine for SocketTest")
        }
      }
    }
  }

  val commonAppleJvmMain = sourceSets.create("commonAppleJvmMain").apply {
    dependsOn(sourceSets.getByName("commonMain"))
    dependencies {
      implementation(libs.ktor.server.core)
      implementation(libs.ktor.server.cio)
      implementation(libs.ktor.server.websockets)
    }
  }
  val commonAppleJvmTest = sourceSets.create("commonAppleJvmTest").apply {
    dependsOn(sourceSets.getByName("commonTest"))
  }
  sourceSets.getByName("jvmMain").dependsOn(commonAppleJvmMain)
  sourceSets.getByName("jvmTest").dependsOn(commonAppleJvmTest)
  sourceSets.getByName("appleMain").dependsOn(commonAppleJvmMain)
  sourceSets.getByName("appleTest").dependsOn(commonAppleJvmTest)
}

