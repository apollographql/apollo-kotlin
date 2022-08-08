plugins {
  kotlin("multiplatform")
}

configureMppDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(okio())
        api(libs.uuid)
        api(projects.apolloAnnotations)
      }
    }
  }
}

val jvmJar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "com.apollographql.apollo3.api")
  }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}
