buildscript {
  project.apply {
    from(rootProject.file("../gradle/dependencies.gradle"))
  }

  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }

  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.oneEightPlugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.android.plugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
    classpath("com.apollographql.apollo3:apollo-gradle-plugin")
  }
}

subprojects {
  repositories {
    google()
    mavenCentral()
    jcenter {
      content {
        includeGroup("org.jetbrains.trove4j")
      }
    }
  }

  afterEvaluate {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
      kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
      }
    }
    (project.extensions.findByName("kotlin")
        as? org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension)?.run {
      sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
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
      if ((name == "jvmTest" || name == "test") && this@subprojects.name !in listOf("kmp-lib-sample", "java-sample", "kotlin-sample", "kmp-android-app")) {
        this@register.dependsOn(this)
      }
    }
  }
}