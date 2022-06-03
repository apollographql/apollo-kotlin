import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

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
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
  rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().nodeVersion = "16.0.0"
}

tasks.register("ciBuild") {
  description = """Execute the 'build' task in subprojects and the `termination:run` task too"""
  subprojects {
    this@register.dependsOn(tasks.matching { it.name == "build" })
  }
  dependsOn(":termination:run")
}

plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
  // graphql-js canarty requires node >= 16.10
  extensions.findByType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension::class.java)?.nodeVersion = "16.10.0"
}