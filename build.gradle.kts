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
    classpath(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
    classpath(groovy.util.Eval.x(project, "x.dep.android.plugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.gradleErrorpronePlugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.gradleJapiCmpPlugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
  }
}

val rootJapiCmp = tasks.register("japicmp")

abstract class DownloadFileTask : DefaultTask() {
  @get:Input
  abstract val url: Property<String>

  @get:org.gradle.api.tasks.OutputFile
  abstract val output: RegularFileProperty

  @TaskAction
  fun taskAction() {
    val client = okhttp3.OkHttpClient()
    val request = okhttp3.Request.Builder().get().url(url.get()).build()

    client.newCall(request).execute().body!!.byteStream().use { body ->
      output.asFile.get().outputStream().buffered().use { file ->
        body.copyTo(file)
      }
    }
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
  }

  plugins.withType(com.android.build.gradle.BasePlugin::class.java) {
    extension.compileOptions {
      sourceCompatibility = JavaVersion.VERSION_1_8
      targetCompatibility = JavaVersion.VERSION_1_8
    }
  }

  plugins.withType(org.gradle.api.plugins.JavaPlugin::class.java) {
    extensions.configure(JavaPluginExtension::class.java) {
      sourceCompatibility = JavaVersion.VERSION_1_8
      targetCompatibility = JavaVersion.VERSION_1_8
    }
  }

  this.apply(plugin = "maven-publish")

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

  val downloadBaselineJarTaskProvider = tasks.register("downloadBaseLineJar", DownloadFileTask::class.java) {
    val group = project.property("GROUP") as String
    val artifact = project.property("POM_ARTIFACT_ID") as String
    val version = "1.2.1"
    val jar = "$artifact-$version.jar"

    url.set("https://jcenter.bintray.com/${group.replace(".", "/")}/$artifact/$version/$jar")
    output.set(File(buildDir, "japicmp/cache/$jar"))
  }

  // TODO: Make this lazy
  this@subprojects.afterEvaluate {
    val jarTask = this@subprojects.tasks.findByName("jar") as? org.gradle.jvm.tasks.Jar
    if (jarTask != null) {
      val japiCmp = this@subprojects.tasks.register("japicmp", me.champeau.gradle.japicmp.JapicmpTask::class.java) {
        dependsOn(downloadBaselineJarTaskProvider)
        oldClasspath = this@subprojects.files(downloadBaselineJarTaskProvider.get().output.asFile.get())
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

  tasks.withType<Test>().configureEach {
    systemProperty("updateTestFixtures", System.getProperty("updateTestFixtures"))
  }

  afterEvaluate {
    configurePublishing()
  }
}

fun Project.configurePublishing() {
  val publicationName = "default"
  val android = extensions.findByType(com.android.build.gradle.BaseExtension::class.java)

  /**
   * Javadoc
   */
  var javadocTask = tasks.findByName("javadoc") as Javadoc?
  var javadocJarTaskProvider: TaskProvider<org.gradle.jvm.tasks.Jar>? = null
  if (javadocTask == null && android != null) {
    javadocTask = tasks.create("javadoc", Javadoc::class.java) {
      source = android.sourceSets["main"].java.sourceFiles
      classpath += project.files(android.getBootClasspath().joinToString(File.pathSeparator))
    }
  }

  if (javadocTask != null) {
    javadocJarTaskProvider = tasks.register("javadocJar", org.gradle.jvm.tasks.Jar::class.java) {
      archiveClassifier.set("javadoc")
      dependsOn(javadocTask)
      from(javadocTask.destinationDir)
    }
  }

  var sourcesJarTaskProvider: TaskProvider<org.gradle.jvm.tasks.Jar>? = null
  val javaPluginConvention = project.convention.findPlugin(JavaPluginConvention::class.java)
  if (javaPluginConvention != null && android == null) {
    sourcesJarTaskProvider = tasks.register("sourcesJar", org.gradle.jvm.tasks.Jar::class.java) {
      archiveClassifier.set("sources")
      from(javaPluginConvention.sourceSets.get("main").allSource)
    }
  } else if (android != null) {
    sourcesJarTaskProvider = tasks.register("sourcesJar", org.gradle.jvm.tasks.Jar::class.java) {
      archiveClassifier.set("sources")
      from(android.sourceSets["main"].java.sourceFiles)
    }
  }

  tasks.withType(Javadoc::class.java) {
    // TODO: fix the javadoc warnings
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
  }

  configure<PublishingExtension> {
    publications {
      if (plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
        withType<MavenPublication>().getByName("jvm") {
          if (javadocJarTaskProvider != null) {
            artifact(javadocJarTaskProvider.get())
          }
          // Keep backward compatibility for JVM users
          artifactId = artifactId.replace("-jvm", "")
        }
        withType<MavenPublication>().getByName("kotlinMultiplatform") {
          // Do not clash with the JVM artifact
          artifactId = "${artifactId}-multiplatform"
        }
      } else {
        create<MavenPublication>(publicationName) {
          val javaComponent = components.findByName("java")
          if (javaComponent != null) {
            from(javaComponent)
          } else if (android != null) {
            // this is a workaround while the below is fixed.
            // dependency information in the pom file will most likely be wrong
            // but it's been like that for some time now. As long as users still
            // import apollo-runtime in addition to the android artifacts, it
            // should be fine.
            //
            // https://issuetracker.google.com/issues/37055147
            // https://github.com/gradle/gradle/pull/8399
            afterEvaluate {
              artifact(tasks.named("bundleReleaseAar").get())
            }
          }

          if (javadocJarTaskProvider != null) {
            artifact(javadocJarTaskProvider.get())
          }
          if (sourcesJarTaskProvider != null) {
            artifact(sourcesJarTaskProvider.get())
          }

          pom {
            artifactId = findProperty("POM_ARTIFACT_ID") as String?
          }
        }
      }
    }

    repositories {
      maven {
        name = "pluginTest"
        url = uri("file://${rootProject.buildDir}/localMaven")
      }

      maven {
        name = "bintray"
        url = uri("https://api.bintray.com/maven/apollographql/android/${project.property("POM_ARTIFACT_ID")}/;publish=1;override=1")
        credentials {
          username = System.getenv("BINTRAY_USER")
          password = System.getenv("BINTRAY_API_KEY")
        }
      }

      maven {
        name = "ojo"
        url = uri("https://oss.jfrog.org/artifactory/oss-snapshot-local/")
        credentials {
          username = System.getenv("BINTRAY_USER")
          password = System.getenv("BINTRAY_API_KEY")
        }
      }

      maven {
        // Same as the regular bintray repository with the plugin marker hardcoded
        name = "bintrayMarker"
        url = uri("https://api.bintray.com/maven/apollographql/android/com.apollographql.apollo.gradle.plugin/;publish=1;override=1")
        credentials {
          username = System.getenv("BINTRAY_USER")
          password = System.getenv("BINTRAY_API_KEY")
        }
      }

      maven {
        name = "oss"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        credentials {
          username = System.getenv("SONATYPE_NEXUS_USERNAME")
          password = System.getenv("SONATYPE_NEXUS_PASSWORD")
        }
      }
    }
  }
}

/**
 * Set fields which are common to all project, either KMP or non-KMP
 */
fun PublicationContainer.setDefaultPomFields() {
  withType<MavenPublication>().all {
    pom {
      groupId = findProperty("GROUP") as String?

      version = findProperty("VERSION_NAME") as String?
      name.set(findProperty("POM_NAME") as String?)
      packaging = findProperty("POM_PACKAGING") as String?

      description.set(findProperty("POM_DESCRIPTION") as String?)
      url.set(findProperty("POM_URL") as String?)

      scm {
        url.set(findProperty("POM_SCM_URL") as String?)
        connection.set(findProperty("POM_SCM_CONNECTION") as String?)
        developerConnection.set(findProperty("POM_SCM_DEV_CONNECTION") as String?)
      }

      licenses {
        license {
          name.set(findProperty("POM_LICENCE_NAME") as String?)
          url.set(findProperty("POM_LICENCE_URL") as String?)
        }
      }

      developers {
        developer {
          id.set(findProperty("POM_DEVELOPER_ID") as String?)
          name.set(findProperty("POM_DEVELOPER_NAME") as String?)
        }
      }
    }
  }
}

tasks.register("publishIfNeeded") {
  val eventName = System.getenv("GITHUB_EVENT_NAME")
  val ref = System.getenv("GITHUB_REF")
  println("publishIfNeeded eventName=$eventName ref=$ref")

  if (eventName == "push" && ref == "ref/heads/master") {
    println("Deploying snapshot to OSS...")
    dependsOn(subprojects.flatMap {
      tasks.matching {
        if (it.name == "apollo-gradle-plugin") {
          it.name in arrayOf("publishDefaultPublicationToOjoRepository",
              "publishApolloGradlePluginPluginMarkerMavenPublicationToOjoRepository")
        } else {
          it.name == "publishAllPublicationsToOjoRepository"
        }
      }
    })
    doLast {
      println("Snapshot deployed on OJO!")
    }
  }

  if (ref.startsWith("ref/tags/")) {
    println("Deploying release to Bintray...")
    dependsOn(subprojects.flatMap {
      tasks.matching {
        if (it.name == "apollo-gradle-plugin") {
          it.name in arrayOf("publishDefaultPublicationToBintrayRepository",
              "publishApolloGradlePluginPluginMarkerMavenPublicationToBintrayMarkerRepository")
        } else {
          it.name == "publishAllPublicationsToBintrayRepository"
        }
      }
    })

    println("Deploying release to Gradle Portal...")
    dependsOn(":apollo-gradle-plugin:publishPlugin")
    doLast {
      println("Release deployed to Bintray and Gradle Plugin Portal!")
    }
  }
}