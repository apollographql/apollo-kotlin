buildscript {
  project.apply {
    from(rootProject.file("gradle/dependencies.gradle"))
  }

  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }

  dependencies {
    classpath("com.apollographql.apollo3:apollo-gradle-plugin")
    classpath("com.apollographql.apollo3:build-logic")
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.allOpen"))
  }
}

subprojects {
  repositories {
    google()
    mavenCentral()
    jcenter {
      content {
        // https://github.com/Kotlin/kotlinx-nodejs/issues/16
        includeModule("org.jetbrains.kotlinx", "kotlinx-nodejs")
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

fun Project.requiresJava9() = name in listOf("jpms")

tasks.register("ciBuildJava8") {
  description = """Execute the 'build' task in Java8 subprojects"""
  subprojects {
    if (!requiresJava9()) {
      this@register.dependsOn(tasks.matching { it.name == "build" })
    }
  }
}

tasks.register("ciBuildJava9") {
  description = """Execute the 'build' task in Java9 subprojects"""
  subprojects {
    if (requiresJava9()) {
      this@register.dependsOn(tasks.matching { it.name == "build" })
    }
  }
}

tasks.register("ciBuild") {
  dependsOn("ciBuildJava8")
  dependsOn("ciBuildJava9")
}
