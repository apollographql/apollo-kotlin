buildscript {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }

  dependencies {
    classpath("com.apollographql.apollo3:apollo-gradle-plugin")
    classpath("com.apollographql.apollo3:build-logic")
  }
}

repositories {
  mavenCentral()
}

subprojects {
  repositories {
    mavenCentral()
    google()
    jcenter {
      content {
        // https://github.com/Kotlin/kotlinx-nodejs/issues/16
        includeModule("org.jetbrains.kotlinx", "kotlinx-nodejs")
      }
    }
  }

  // Workaround for https://youtrack.jetbrains.com/issue/KT-51970
  afterEvaluate {
    afterEvaluate {
      tasks.configureEach {
        if (
            name.startsWith("compile")
            && name.endsWith("KotlinMetadata")
        ) {
          enabled = false
        }
      }
    }
  }

  configureJavaAndKotlinCompilers()

  afterEvaluate {
    tasks.withType<AbstractTestTask> {
      testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        events.add(org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED)
        events.add(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
        showStandardStreams = true
      }
    }
  }
}

// See https://youtrack.jetbrains.com/issue/KT-49109#focus=Comments-27-5259190.0-0
rootProject.configureNode()

tasks.register("ciBuild") {
  description = """Execute the 'build' task in subprojects and the `termination:run` task too"""
  subprojects {
    this@register.dependsOn(tasks.matching { it.name == "build" })
  }
  dependsOn(":termination:run")
  doLast {
    checkGitStatus()
  }
}
