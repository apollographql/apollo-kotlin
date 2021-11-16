plugins {
  kotlin("multiplatform")
}

configureMppDefaults(withLinux = false)

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.apolloApi)
        api(projects.apolloMppUtils)
        api(projects.apolloRuntime)
        api(projects.apolloNormalizedCacheCommon)
        api(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
      }
    }
  }
}

val jvmJar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "com.apollographql.apollo3.cache.normalized")
  }
}