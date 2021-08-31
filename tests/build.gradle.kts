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
      }
    }
  }
}

tasks.register("fullCheck") {
  dependsOn(gradle.includedBuild("apollo-android").task(":fullCheck"))
  subprojects {
    tasks.all {
      if (this.name == "build") {
        this@register.dependsOn(this)
      }
    }
  }
}

/**
 * A task to do (relatively) fast checks when iterating
 * It only does JVM and skip samples and compiler/gradle tests
 */
tasks.register("quickCheck") {
  dependsOn(gradle.includedBuild("apollo-android").task(":quickCheck"))
  subprojects {
    tasks.all {
      if (this@subprojects.name in listOf("kmp-lib-sample", "java-sample", "kotlin-sample", "kmp-android-app")) {
        // These are super long to execute, keep them for the full check
        return@all
      }
      if (this is org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest) {
        // Native is slow, keep the for the full check
        return@all
      }
      if (this is Test) {
        // run all tests
        this@register.dependsOn(this)
      }
    }
  }
}
