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
    classpath("com.apollographql.apollo:build-logic")
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.springPlugin"))
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

tasks.register("ciTestsIntegration") {
  description = """Execute the 'build' task in each subproject"""
  subprojects {
    this@register.dependsOn(tasks.matching { it.name == "build" })
  }
}

tasks.register("ciFull") {
  dependsOn(gradle.includedBuild("apollo-android").task(":ciFull"))
  dependsOn("ciTestsIntegration")
}

