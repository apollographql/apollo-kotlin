plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apply(plugin = "com.android.library")
apply(plugin = "com.squareup.sqldelight")

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.cache.normalized.sql")
  mpp {
    withLinux.set(false)
    // https://github.com/cashapp/sqldelight/pull/1486
    withJs.set(false)
    withAndroid.set(true)
  }
}

configure<com.squareup.sqldelight.gradle.SqlDelightExtension> {
  database("JsonDatabase") {
    packageName = "com.apollographql.apollo3.cache.normalized.sql.internal.json"
    schemaOutputDirectory = file("src/commonMain/sqldelight/json/schema")
    sourceFolders = listOf("sqldelight/json/")
  }
}

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-normalized-cache-api"))
        api(project(":apollo-normalized-cache"))
        api(golatac.lib("sqldelight.runtime"))
      }
    }

    findByName("jvmMain")?.apply {
      dependencies {
        implementation(golatac.lib("sqldelight.jvm"))
      }
    }

    findByName("appleMain")?.apply {
      dependencies {
        implementation(golatac.lib("sqldelight.native"))
      }
    }

    findByName("jvmTest")?.apply {
      dependencies {
        implementation(golatac.lib("truth"))
      }
    }

    findByName("androidMain")?.apply {
      dependencies {
        api(golatac.lib("androidx.sqlite"))
        implementation(golatac.lib("sqldelight.android"))
        implementation(golatac.lib("androidx.sqlite.framework"))
        implementation(golatac.lib("androidx.startup.runtime"))
      }
    }

    findByName("androidTest")?.apply {
      dependencies {
        implementation(golatac.lib("kotlin.test.junit"))
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(project(":apollo-testing-support"))
      }
    }
  }
}

configure<com.android.build.gradle.LibraryExtension> {
  compileSdk = golatac.version("android.sdkversion.compile").toInt()

  defaultConfig {
    minSdk = golatac.version("android.sdkversion.min").toInt()
    targetSdk = golatac.version("android.sdkversion.target").toInt()
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
