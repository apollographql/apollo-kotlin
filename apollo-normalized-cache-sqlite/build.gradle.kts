if (System.getProperty("idea.sync.active") == null) {
  apply(plugin = "com.android.library")
}
apply(plugin = "org.jetbrains.kotlin.multiplatform")
apply(plugin = "com.squareup.sqldelight")

configure<com.squareup.sqldelight.gradle.SqlDelightExtension> {
  database("ApolloDatabase") {
    packageName = "com.apollographql.apollo3.cache.normalized.sql"
    schemaOutputDirectory = file("src/main/sqldelight/schemas")
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
      }
    }

    val jvmMain by getting {
      dependsOn(commonMain)
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.sqldelight.jvm"))
      }
    }

    val appleMain by getting {
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.sqldelight.native"))
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
          api(groovy.util.Eval.x(project, "x.dep.androidx.sqlite"))
          implementation(groovy.util.Eval.x(project, "x.dep.sqldelight.android"))
          implementation(groovy.util.Eval.x(project, "x.dep.androidx.sqliteFramework"))
        }
      }
      val androidTest by getting {
        // this allows the android unit test to use the JVM driver
        // TODO: makes this better with HMPP?
        dependsOn(jvmTest)
        dependencies {
          implementation(kotlin("test-junit"))
        }
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