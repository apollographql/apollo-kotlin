plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apply(plugin = "com.android.library")
apply(plugin = "com.squareup.sqldelight")

apolloLibrary {
  javaModuleName.set("com.apollographql.apollo3.cache.normalized.sql")
  mpp {
    withLinux.set(false)
    // https://github.com/cashapp/sqldelight/pull/1486
    withJs.set(false)
  }
}

configure<com.squareup.sqldelight.gradle.SqlDelightExtension> {
  database("JsonDatabase") {
    packageName = "com.apollographql.apollo3.cache.normalized.sql.internal.json"
    schemaOutputDirectory = file("sqldelight/json/schema")
    sourceFolders = listOf("sqldelight/json/")
  }
  database("BlobDatabase") {
    packageName = "com.apollographql.apollo3.cache.normalized.sql.internal.blob"
    schemaOutputDirectory = file("sqldelight/blob/schema")
    sourceFolders = listOf("sqldelight/blob/")
  }
  database("Blob2Database") {
    packageName = "com.apollographql.apollo3.cache.normalized.sql.internal.blob2"
    schemaOutputDirectory = file("src/commonMain/sqldelight/blob2/schema")
    sourceFolders = listOf("sqldelight/blob2/")
  }
}

kotlin {
  android {
    publishAllLibraryVariants()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.apolloApi)
        api(projects.apolloNormalizedCacheApiIncubating)
        api(projects.apolloNormalizedCacheIncubating)
      }
    }

    val jvmMain by getting {
      dependsOn(commonMain)
      dependencies {
        implementation(libs.sqldelight.jvm)
      }
    }

    val appleMain by getting {
      dependencies {
        implementation(libs.sqldelight.native)
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(libs.truth)
      }
    }

    val androidMain by getting {
      dependsOn(commonMain)
      dependencies {
        api(libs.androidx.sqlite)
        implementation(libs.sqldelight.android)
        implementation(libs.androidx.sqlite.framework)
        implementation(libs.androidx.startup.runtime)
      }
    }
    val androidTest by getting {
      dependencies {
        implementation(libs.kotlin.test.junit)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(projects.apolloTestingSupport)
      }
    }
  }
}

configure<com.android.build.gradle.LibraryExtension> {
  compileSdk = libs.versions.android.sdkversion.compile.get().toInt()

  defaultConfig {
    minSdk = libs.versions.android.sdkversion.min.get().toInt()
    targetSdk = libs.versions.android.sdkversion.target.get().toInt()
  }
}


tasks.named("lint") {
  /**
   * lint warns with:
   *
   * ```
   * Could not load custom lint check jar file /Users/mbonnin/.gradle/caches/transforms-3/a58c406cc84b74815c738fa583c867e0/transformed/startup-runtime-1.1.1/jars/lint.jar
   * java.lang.NoClassDefFoundError: com/android/tools/lint/client/api/Vendor
   * ```
   *
   * In general, there is so little Android code here, it's not really worth running lint
   */
  enabled = false
}

tasks.configureEach {
  if (name.endsWith("UnitTest")) {
    /**
     * Because there is no App Startup in Android unit tests, the Android tests
     * fail at runtime so ignore them
     * We could make the Android unit tests use the Jdbc driver if we really wanted to
     */
    enabled = false
  }
}
