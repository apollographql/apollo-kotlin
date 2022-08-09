plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

configureMppDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(libs.kotlin.stdlib)
      }
    }

    val jsMain by getting {
      dependencies {
        // See https://youtrack.jetbrains.com/issue/KT-53471
        api(libs.kotlin.stdlib.js)
      }
    }

  }
}

val jvmJar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "com.apollographql.apollo3.annotations")
  }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}
