plugins {
  kotlin("multiplatform")
}

configureMppDefaults(withLinux = false)

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.apolloRuntime)
        api(projects.apolloNormalizedCacheApi)
        api(groovy.util.Eval.x(project, "x.dep.kotlinCoroutines"))
        implementation(groovy.util.Eval.x(project, "x.dep.atomicfu").toString()) {
          because("Use of ReentrantLock in DefaultApolloStore for Apple (we don't use the gradle plugin rewrite)")
        }
      }
    }
  }
}

val jvmJar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "com.apollographql.apollo3.cache.normalized")
  }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}
