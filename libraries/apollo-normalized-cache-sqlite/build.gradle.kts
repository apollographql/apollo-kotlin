plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apply(plugin = "app.cash.sqldelight")

apolloLibrary(
    namespace = "com.apollographql.apollo.cache.normalized.sql",
    withLinux = false,
    withJs = false, // https://github.com/cashapp/sqldelight/pull/1486
    withWasm = false,
    androidOptions = AndroidOptions(withCompose = false)
)

configure<app.cash.sqldelight.gradle.SqlDelightExtension> {
  databases.create("JsonDatabase") {
    packageName = "com.apollographql.apollo.cache.normalized.sql.internal.json"
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
        api(libs.sqldelight.runtime)
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
        implementation(libs.slf4j.nop)
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
