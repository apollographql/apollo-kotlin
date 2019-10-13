buildscript {
  project.apply {
    from(rootProject.file("gradle/dependencies.gradle"))
  }

  repositories {
    maven { url = uri("https://plugins.gradle.org/m2/") }
    google()
  }

  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.android.plugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.gradleErrorpronePlugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
  }
}

subprojects {
  apply {
    from(rootProject.file("gradle/dependencies.gradle"))
  }

  buildscript {
    repositories {
      maven { url = uri("https://plugins.gradle.org/m2/") }
      maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
      google()
    }

    dependencies {
      classpath(groovy.util.Eval.x(project, "x.dep.bintrayGradlePlugin"))
      classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
    }
  }

  repositories {
    maven { url = uri("https://plugins.gradle.org/m2/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    google()
    maven { url = uri("https://jitpack.io") }
  }

  apply(plugin = "net.ltgt.errorprone")

  configurations.named("errorprone") {
    resolutionStrategy.force(groovy.util.Eval.x(this@subprojects, "x.dep.errorProneCore"))
  }

  group = property("GROUP")!!
  version = property("VERSION_NAME")!!

  if (project.name != "apollo-gradle-plugin") {
    apply(plugin = "checkstyle")

    extensions.findByType(CheckstyleExtension::class.java)!!.apply {
      configFile = rootProject.file("checkstyle.xml")
      configProperties = mapOf(
          "checkstyle.cache.file" to rootProject.file("build/checkstyle.cache")
      )
    }

    tasks.register("checkstyle", Checkstyle::class.java) {
      source("src/main/java")
      include("**/*.java")
      classpath = files()
    }

    tasks.withType<JavaCompile>().configureEach {
      options.compilerArgs.add("-XepDisableWarningsInGeneratedCode")
    }

    afterEvaluate {
      tasks.findByName("check")?.dependsOn("checkstyle")
    }
  }
  tasks.withType<Test>().configureEach {
    systemProperty("updateTestFixtures", System.getProperty("updateTestFixtures"))
  }
}
