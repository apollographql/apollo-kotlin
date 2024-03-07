plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apply(plugin = "app.cash.sqldelight")

apolloLibrary(
    namespace = "com.apollographql.apollo3.cache.normalized.sql",
    withLinux = false,
    withJs = false, // https://github.com/cashapp/sqldelight/pull/1486,
    withWasm = false,
    androidOptions = AndroidOptions(withCompose = false),
)

configure<app.cash.sqldelight.gradle.SqlDelightExtension> {
  databases.create("JsonDatabase") {
    packageName = "com.apollographql.apollo3.cache.normalized.sql.internal.json"
    schemaOutputDirectory = file("sqldelight/json/schema")
    srcDirs("src/commonMain/sqldelight/json/")
  }
  databases.create("BlobDatabase") {
    packageName = "com.apollographql.apollo3.cache.normalized.sql.internal.blob"
    schemaOutputDirectory = file("sqldelight/blob/schema")
    srcDirs("src/commonMain/sqldelight/blob/")
  }
  databases.create("Blob2Database") {
    packageName = "com.apollographql.apollo3.cache.normalized.sql.internal.blob2"
    schemaOutputDirectory = file("sqldelight/blob2/schema")
    srcDirs("src/commonMain/sqldelight/blob2/")
  }
}

kotlin {
  androidTarget {
    publishAllLibraryVariants()
  }

  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        api(project(":apollo-api"))
        api(project(":apollo-normalized-cache-api-incubating"))
        api(project(":apollo-normalized-cache-incubating"))
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
