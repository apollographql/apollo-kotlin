buildscript {
  project.apply {
    from(rootProject.file("gradle/dependencies.gradle"))
  }

  repositories {
    maven { url = uri("https://plugins.gradle.org/m2/") }
    google()
    jcenter()
  }

  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.android.plugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.gradleErrorpronePlugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.gradleJapiCmpPlugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.bintrayGradlePlugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
  }
}

fun baselineJar(project: Project, version: String): File {
  val group = project.property("GROUP")
  val artifactId = project.property("POM_ARTIFACT_ID")
  try {
    val jarFile = "$artifactId-${version}.jar"
    project.group = "virtual_group_for_japicmp" // Prevent it from resolving the current version.
    // see https://github.com/apollographql/apollo-android/issues/1325
    project.repositories.maven("https://dl.bintray.com/apollographql/android")
    val dependency = project.dependencies.create("$group:$artifactId:$version@jar")
    return project.configurations.detachedConfiguration(dependency).files
        .first { (it.name == jarFile) }
  } finally {
    project.group = group!!
  }
}

val rootJapiCmp = tasks.register("japicmp")

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

  // TODO: Make this lazy
  this@subprojects.afterEvaluate {
    val jarTask = this@subprojects.tasks.findByName("jar") as? org.gradle.jvm.tasks.Jar
    if (jarTask != null) {
      val japiCmp = this@subprojects.tasks.register("japicmp", me.champeau.gradle.japicmp.JapicmpTask::class.java) {
        oldClasspath = this@subprojects.files(baselineJar(this@subprojects, "1.2.1"))
        newClasspath = this@subprojects.files(jarTask.archiveFile)
        ignoreMissingClasses = true
        packageExcludes = listOf("*.internal*")
        onlyModified = true
        txtOutputFile = this@subprojects.file("$buildDir/reports/japi.txt")
      }
      rootJapiCmp.configure {
        dependsOn(japiCmp)
      }
    }
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
