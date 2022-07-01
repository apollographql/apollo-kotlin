if (System.getProperty("idea.sync.active") == null) {
  apply(plugin = "com.android.library")
}
apply(plugin = "org.jetbrains.kotlin.multiplatform")
apply(plugin = "com.squareup.sqldelight")

configure<com.squareup.sqldelight.gradle.SqlDelightExtension> {
  database("JsonDatabase") {
    packageName = "com.apollographql.apollo3.cache.internal.json"
    schemaOutputDirectory = file("sqldelight/json/schema")
    sourceFolders = listOf("sqldelight/json/")
  }
  database("BlobDatabase") {
    packageName = "com.apollographql.apollo3.cache.internal.blob"
    schemaOutputDirectory = file("sqldelight/blob/schema")
    sourceFolders = listOf("sqldelight/blob/")
  }
}

// https://github.com/cashapp/sqldelight/pull/1486
configureMppDefaults(withJs = false, withLinux = false)

configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
  if (System.getProperty("idea.sync.active") == null) {
    android {
      publishAllLibraryVariants()
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.apolloApi)
        api(projects.apolloNormalizedCacheApi)
        api(projects.apolloNormalizedCache)
      }
    }

    val jvmMain by getting {
      dependsOn(commonMain)
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.sqldelightJvm"))
      }
    }

    val appleMain by getting {
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.sqldelightNative"))
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.truth"))
      }
    }

    if (System.getProperty("idea.sync.active") == null) {
      val androidMain by getting {
        dependsOn(commonMain)
        dependencies {
          api(groovy.util.Eval.x(project, "x.dep.androidxSqlite"))
          implementation(groovy.util.Eval.x(project, "x.dep.sqldelightAndroid"))
          implementation(groovy.util.Eval.x(project, "x.dep.androidxSqliteFramework"))
          implementation(groovy.util.Eval.x(project, "x.dep.androidxStartupRuntime"))
        }
      }
      val androidTest by getting {
        dependencies {
          implementation(kotlin("test-junit"))
        }
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(projects.apolloTestingSupport)
      }
    }
  }
}

if (System.getProperty("idea.sync.active") == null) {
  configure<com.android.build.gradle.LibraryExtension> {
    compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

    defaultConfig {
      minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString())
      targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString())
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
}

val jvmJar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "com.apollographql.apollo3.cache.normalized.sql")
  }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}
