plugins {
  kotlin("multiplatform")
}

configureMppDefaults(withLinux = false)

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.apolloAnnotations)
        api(okio())
        implementation(libs.atomicfu.get().toString()) {
          because("We need locks for native (we don't use the gradle plugin rewrite)")
        }
        implementation(libs.kotlinx.coroutines)
      }
    }

    val jsMain by getting {
      dependencies {
        implementation(libs.kotlinx.nodejs)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(projects.apolloTestingSupport) {
          because("runTest")
        }
        implementation(projects.apolloRuntime) {
          because("We need HttpEngine for SocketTest")
        }
      }
    }
  }
}

val jvmJar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "com.apollographql.apollo3.mockserver")
  }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}
