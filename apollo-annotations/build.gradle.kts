plugins {
  kotlin("multiplatform")
}

configureMppDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
    }
  }
}

val jvmJar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "com.apollographql.apollo3.annotations")
  }
}