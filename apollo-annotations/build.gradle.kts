plugins {
  kotlin("multiplatform")
}

configureMppDefaults()

kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(kotlin("stdlib", groovy.util.Eval.x(project, "x.versions.kotlinStdlib").toString()))
      }
    }

    val jsMain by getting {
      dependencies {
        api(kotlin("stdlib-js", groovy.util.Eval.x(project, "x.versions.kotlinStdlib").toString()))
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
