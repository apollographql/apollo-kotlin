
import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.BaseExtension
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters
import dev.adamko.dokkatoo.dokka.plugins.DokkaVersioningPluginParameters
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
import kotlinx.coroutines.runBlocking
import net.mbonnin.vespene.lib.NexusStagingClient
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

fun Project.configurePublishing(isAggregateKdoc: Boolean = false) {
  apply {
    plugin("signing")
  }
  apply {
    plugin("maven-publish")
  }

  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    configureDokka()
  }
  pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    configureDokka()
  }
  pluginManager.withPlugin("com.android.library") {
    extensions.findByType(LibraryExtension::class.java)!!.publishing {
      singleVariant("release")
    }
  }

  if (isAggregateKdoc) {
    configureDokkaAggregate()
  }
  configurePublishingInternal()
}

fun Project.configureDokkaCommon(): DokkatooExtension {
  apply {
    plugin("dev.adamko.dokkatoo-html")
  }
  val dokkatoo = extensions.getByType(DokkatooExtension::class.java)

  dokkatoo.apply {
    pluginsConfiguration.getByName("html") {
      this as DokkaHtmlPluginParameters
      customStyleSheets.from(
          listOf("style.css", "prism.css", "logo-styles.css").map { rootProject.file("dokka/$it") }
      )
      customAssets.from(
          listOf("apollo.svg").map { rootProject.file("dokka/$it") }
      )
    }
  }

  tasks.withType(DokkatooGenerateTask::class.java).configureEach {
    /**
     * Speed up development. When running the Gradle integration tests, we don't need KDoc to be generated
     */
    onlyIf {
      if (gradle.taskGraph.allTasks.any {
            it.name == "publishAllPublicationsToPluginTestRepository" ||
                it.name == "publishToMavenLocal"
          }) {
        return@onlyIf false
      }
      true
    }

    workerMaxHeapSize.set("8g")
  }

  dokkatoo.dokkatooSourceSets.configureEach {
    includes.from("README.md")
  }

  return dokkatoo
}

fun Project.configureDokka() {
  configureDokkaCommon()
  val project = this
  val kdocProject = project(":apollo-kdoc")
  kdocProject.configurations.all {
    if (name == "dokkatoo" && project.name == "apollo-annotations") {
      dependencies.add(kdocProject.dependencies.project(mapOf("path" to project.path)))
    }
  }
}

fun Project.configureDokkaAggregate() {
  val dokkatoo = configureDokkaCommon()
  dependencies.add(
      "dokkatooPluginHtml",
      dokkatoo.versions.jetbrainsDokka.map { dokkaVersion ->
        "org.jetbrains.dokka:all-modules-page-plugin:$dokkaVersion"
      }
  )
  dependencies.add(
      "dokkatooPluginHtml",
      dokkatoo.versions.jetbrainsDokka.map { dokkaVersion ->
        "org.jetbrains.dokka:versioning-plugin:$dokkaVersion"
      }
  )

  val kdocVersionTasks = listOf<String>().map {version ->
    val versionString = version.replace(".", "_").replace("-", "_")
    val configuration = configurations.create("apolloKdocVersion_$versionString") {
      isCanBeResolved = true
      isCanBeConsumed = false
      isTransitive = false

      dependencies.add(project.dependencies.create("com.apollographql.apollo3:apollo-api:$version:javadoc"))
    }

    tasks.register("extractApolloKdocVersion_$versionString", Copy::class.java) {
      from(configuration.map { zipTree(it) })
      into(layout.buildDirectory.dir("kdoc-versions/$version"))
    }
  }

  val downloadKDocVersions = tasks.register("dowloadKDocVersions") {
    dependsOn(kdocVersionTasks)
    outputs.dir(layout.buildDirectory.dir("kdoc-versions/"))
    doLast {
      // Make sure the folder is created
      outputs.files.singleFile.mkdirs()
    }
  }

  dokkatoo.pluginsConfiguration.getByName("versioning") {
    this as DokkaVersioningPluginParameters
    olderVersionsDir.fileProvider(downloadKDocVersions.map { it.outputs.files.singleFile })
  }
}

private fun Project.getOssStagingUrl(): String {
  val url = try {
    this.extensions.extraProperties["ossStagingUrl"] as String?
  } catch (e: ExtraPropertiesExtension.UnknownPropertyException) {
    null
  }
  if (url != null) {
    return url
  }
  val baseUrl = "https://s01.oss.sonatype.org/service/local/"
  val client = NexusStagingClient(
      baseUrl = baseUrl,
      username = System.getenv("SONATYPE_NEXUS_USERNAME"),
      password = System.getenv("SONATYPE_NEXUS_PASSWORD"),
  )
  val repositoryId = runBlocking {
    client.createRepository(
        profileId = System.getenv("COM_APOLLOGRAPHQL_PROFILE_ID"),
        description = "apollo-kotlin $version"
    )
  }
  return "${baseUrl}staging/deployByRepositoryId/${repositoryId}/".also {
    this.extensions.extraProperties["ossStagingUrl"] = it
  }
}

private fun Project.configurePublishingInternal() {
  /**
   * Javadoc
   */
  val dokkaJarTaskProvider = tasks.register("defaultJavadocJar", org.gradle.jvm.tasks.Jar::class.java) {
    archiveClassifier.set("javadoc")

    /**
     * Dokka is not enabled for Android projects and dokkatooGeneratePublicationHtml is not found
     */
    runCatching {
      from(tasks.named("dokkatooGeneratePublicationHtml").map { (it as DokkatooGenerateTask).outputDirectory.get().asFile })
    }
  }

  val emptyJavadocJarTaskProvider = tasks.register("emptyJavadocJar", org.gradle.jvm.tasks.Jar::class.java) {
    // Add an appendix to avoid the output of this task to overlap with defaultJavadocJar
    archiveAppendix.set("empty")
    archiveClassifier.set("javadoc")
  }

  tasks.withType(Jar::class.java) {
    manifest {
      attributes["Built-By"] = findProperty("POM_DEVELOPER_ID") as String?
      attributes["Created-By"] = "Gradle ${gradle.gradleVersion}"
      attributes["Implementation-Title"] = findProperty("POM_NAME") as String?
      attributes["Implementation-Version"] = findProperty("VERSION_NAME") as String?
    }
  }

  extensions.configure(PublishingExtension::class.java) {
    publications {
      when {
        plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") -> {
          /**
           * Kotlin MPP creates nice publications.
           * It only misses the javadoc
           */
          withType(MavenPublication::class.java).configureEach {
            if (name == "kotlinMultiplatform") {
              // Add the javadoc to the multiplatform publications
              artifact(dokkaJarTaskProvider)
            } else {
              // And an empty one for others so as to save some space
              artifact(emptyJavadocJarTaskProvider)
            }
          }
        }

        plugins.hasPlugin("com.gradle.plugin-publish") -> {
          /**
           * com.gradle.plugin-publish creates all publications
           */
        }

        plugins.hasPlugin("java-gradle-plugin") -> {
          /**
           * java-gradle-plugin creates 2 publications (one marker and one regular) but without source/javadoc.
           */
          withType(MavenPublication::class.java) {
            // Only add sources and javadoc for the main publication
            if (!name.lowercase().contains("marker")) {
              artifact(dokkaJarTaskProvider)
              artifact(createJavaSourcesTask())
            }
          }
        }

        extensions.findByName("android") != null -> {
          /**
           * Android projects do not create publications (yet?). Do it ourselves.
           */
          create("default", MavenPublication::class.java) {
            afterEvaluate {
              // afterEvaluate is required for Android
              from(components.findByName("release"))
            }

            artifact(dokkaJarTaskProvider)
            artifact(createAndroidSourcesTask())

            artifactId = project.name
          }
        }

        extensions.findByName("java") != null -> {
          /**
           * Kotlin JVM do not create publications (yet?). Do it ourselves.
           */
          create("default", MavenPublication::class.java) {

            from(components.findByName("java"))
            artifact(dokkaJarTaskProvider)
            artifact(createJavaSourcesTask())

            artifactId = project.name
          }
        }

        else -> {
          /**
           * No plugin applied -> this is the aggregate publication
           */
          create("default", MavenPublication::class.java) {
            artifact(dokkaJarTaskProvider)

            artifactId = project.name
          }
        }
      }

      /**
       * In all cases, configure the pom
       */
      withType(MavenPublication::class.java).configureEach {
        setDefaultPomFields(this)
      }
    }

    repositories {
      maven {
        name = "pluginTest"
        url = uri(rootProject.layout.buildDirectory.dir("localMaven"))
      }

      maven {
        name = "ossSnapshots"
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        credentials {
          username = System.getenv("SONATYPE_NEXUS_USERNAME")
          password = System.getenv("SONATYPE_NEXUS_PASSWORD")
        }
      }

      maven {
        name = "ossStaging"
        setUrl {
          uri(rootProject.getOssStagingUrl())
        }
        credentials {
          username = System.getenv("SONATYPE_NEXUS_USERNAME")
          password = System.getenv("SONATYPE_NEXUS_PASSWORD")
        }
      }

      maven {
        name = "repsy"
        setUrl("https://repo.repsy.io/mvn/mbonnin/default")
        credentials {
          username = System.getenv("REPSY_USERNAME")
          password = System.getenv("REPSY_PASSWORD")
        }
      }
    }
  }

  // See https://youtrack.jetbrains.com/issue/KT-46466/Kotlin-MPP-publishing-Gradle-7-disables-optimizations-because-of-task-dependencies#focus=Comments-27-7102038.0-0
  val signingTasks = tasks.withType(Sign::class.java)
  tasks.withType(AbstractPublishToMaven::class.java).configureEach {
    this.dependsOn(signingTasks)
  }

  extensions.configure(SigningExtension::class.java) {
    // GPG_PRIVATE_KEY should contain the armoured private key that starts with -----BEGIN PGP PRIVATE KEY BLOCK-----
    // It can be obtained with gpg --armour --export-secret-keys KEY_ID
    useInMemoryPgpKeys(System.getenv("GPG_PRIVATE_KEY"), System.getenv("GPG_PRIVATE_KEY_PASSWORD"))
    val publicationsContainer = (extensions.getByName("publishing") as PublishingExtension).publications
    sign(publicationsContainer)
  }
  tasks.withType(Sign::class.java).configureEach {
    isEnabled = !System.getenv("GPG_PRIVATE_KEY").isNullOrBlank()
  }

  // https://github.com/gradle/gradle/issues/26132
  afterEvaluate {
    tasks.all {
      if (name.startsWith("compileTestKotlin")) {
        val target = name.substring("compileTestKotlin".length)
        val sign = try {
          tasks.named("sign${target}Publication")
        } catch (e: Throwable) {
          null
        }
        if (sign != null) {
          dependsOn(sign)
        }
      }
    }
  }
}


/**
 * Set fields which are common to all project, either KMP or non-KMP
 */
private fun Project.setDefaultPomFields(mavenPublication: MavenPublication) {
  mavenPublication.groupId = findProperty("GROUP") as String?
  mavenPublication.version = findProperty("VERSION_NAME") as String?

  mavenPublication.pom {
    name.set(findProperty("POM_NAME") as String?)
    (findProperty("POM_PACKAGING") as String?)?.let {
      // Do not overwrite packaging if already set by the multiplatform plugin
      packaging = it
    }

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

private fun Project.createJavaSourcesTask(): TaskProvider<Jar> {
  return tasks.register("javaSourcesJar", Jar::class.java) {
    archiveClassifier.set("sources")
    val sourceSets = project.extensions.getByType(JavaPluginExtension::class.java).sourceSets
    from(sourceSets.getByName("main").allSource)
  }
}

private fun Project.createAndroidSourcesTask(): TaskProvider<Jar> {
  return tasks.register("javaSourcesJar", Jar::class.java) {
    val android = project.extensions.findByName("android") as BaseExtension
    from(android.sourceSets.getByName("main").java.getSourceFiles())
    archiveClassifier.set("sources")
  }
}
