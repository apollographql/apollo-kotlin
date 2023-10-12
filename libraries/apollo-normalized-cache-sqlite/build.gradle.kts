plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apply(plugin = "com.android.library")
apply(plugin = "app.cash.sqldelight")

apolloLibrary(
    javaModuleName = "com.apollographql.apollo3.cache.normalized.sql",
    withLinux = false,
    withJs = false, // https://github.com/cashapp/sqldelight/pull/1486
)

configure<app.cash.sqldelight.gradle.SqlDelightExtension> {
  databases.create("JsonDatabase") {
    packageName = "com.apollographql.apollo3.cache.normalized.sql.internal.json"
    schemaOutputDirectory = file("sqldelight/json/schema")
    srcDirs("src/commonMain/sqldelight/json/")
  }
}

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-normalized-cache-api"))
        api(project(":apollo-normalized-cache"))
      }
    }

    findByName("jvmMain")?.apply {
      dependencies {
        implementation(libs.sqldelight.jvm)
      }
    }

    findByName("appleMain")?.apply {
      dependencies {
        implementation(libs.sqldelight.native)
      }
    }

    findByName("jvmTest")?.apply {
      dependencies {
        implementation(libs.truth)
      }
    }

    findByName("androidMain")?.apply {
      dependencies {
        api(libs.androidx.sqlite)
        implementation(libs.sqldelight.android)
        implementation(libs.androidx.sqlite.framework)
        implementation(libs.androidx.startup.runtime)
      }
    }

    findByName("androidTest")?.apply {
      dependencies {
        implementation(libs.kotlin.test.junit)
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
  compileSdk = libs.versions.android.sdkversion.compile.get().toInt()
  namespace = "com.apollographql.apollo3.cache.normalized.sql"

  defaultConfig {
    minSdk = libs.versions.android.sdkversion.min.get().toInt()
  }
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
