plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
}

apply(plugin = "com.android.library")
apply(plugin = "app.cash.sqldelight")

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.cache.normalized.sql")
  mpp {
    withLinux.set(false)
    // https://github.com/cashapp/sqldelight/pull/1486
    withJs.set(false)
  }
}

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
  namespace = "com.apollographql.apollo3.cache.normalized.sql"

  defaultConfig {
    minSdk = golatac.version("android.sdkversion.min").toInt()
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
