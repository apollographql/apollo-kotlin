import com.android.build.gradle.BaseExtension
import kotlinx.coroutines.runBlocking
import net.mbonnin.vespene.lib.NexusStagingClient
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import com.android.build.api.dsl.LibraryExtension

fun Project.configurePublishing() {
  if (
      name in setOf(
          "apollo-runtime-java",
          "apollo-rx2-support-java",
          "apollo-rx3-support-java",
      )
  ) {
    return
  }

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
  configurePublishingInternal()
}

private fun Project.configureDokka() {
  apply {
    plugin("org.jetbrains.dokka")
  }

  tasks.withType(DokkaTask::class.java).configureEach {
    //https://github.com/Kotlin/dokka/issues/1455
    dependsOn("assemble")
  }
  tasks.withType(DokkaTaskPartial::class.java).configureEach {
    //https://github.com/Kotlin/dokka/issues/1455
    dependsOn("assemble")
  }

  tasks.withType(AbstractDokkaTask::class.java).configureEach {
    pluginConfiguration<org.jetbrains.dokka.base.DokkaBase, org.jetbrains.dokka.base.DokkaBaseConfiguration> {
      customAssets = listOf("apollo.svg").map { rootProject.file("dokka/$it") }
      customStyleSheets = listOf("style.css", "prism.css", "logo-styles.css").map { rootProject.file("dokka/$it") }
    }
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

    runCatching {
      from(tasks.named("dokkaHtml").flatMap { (it as DokkaTask).outputDirectory })
    }
  }
  val emptyJavadocJarTaskProvider = tasks.register("emptyJavadocJar", org.gradle.jvm.tasks.Jar::class.java) {
    // Add an appendix to avoid the output of this task to overlap with defaultJavadocJar
    archiveAppendix.set("empty")
    archiveClassifier.set("javadoc")
  }

  /**
   * Type `echo "apollographql_publish_kdoc=false" >> ~/.gradle/gradle.properties` on your development machine
   * to save some time during Gradle tests and publishing to mavenLocal
   */
  val javadocJarTaskProvider = if (properties["apollographql_publish_kdoc"] == "false") {
    emptyJavadocJarTaskProvider
  } else {
    dokkaJarTaskProvider
  }

  tasks.withType(Jar::class.java) {
    manifest {
      attributes["Built-By"] = findProperty("POM_DEVELOPER_ID") as String?
      attributes["Build-Jdk"] = "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")})"
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
              artifact(javadocJarTaskProvider)
            } else {
              // And an empty one for others so as to save some space
              artifact(emptyJavadocJarTaskProvider)
            }
          }
        }

        plugins.hasPlugin("java-gradle-plugin") -> {
          /**
           * java-gradle-plugin creates 2 publications (one marker and one regular) but without source/javadoc.
           */
          withType(MavenPublication::class.java) {
            artifact(javadocJarTaskProvider)
            // Only add sources for the main publication
            // XXX: is there a nicer way to do this?
            if (!name.lowercase().contains("marker")) {
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

            artifact(javadocJarTaskProvider)
            artifact(createAndroidSourcesTask())

            artifactId = findProperty("POM_ARTIFACT_ID") as String?
          }
        }

        components.findByName("java") != null -> {
          /**
           * Kotlin JVM do not create publications (yet?). Do it ourselves.
           */
          create("default", MavenPublication::class.java) {

            from(components.findByName("java"))
            artifact(javadocJarTaskProvider)
            artifact(createJavaSourcesTask())

            artifactId = findProperty("POM_ARTIFACT_ID") as String?
          }
        }
        else -> {
          /**
           * apollo-kdoc
           */
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
        url = uri("file://${rootProject.buildDir}/localMaven")
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
    /**
     * Add a dependency on the compileKotlin task to make sure the generated sources like
     * antlr or SQLDelight get included
     * See also https://youtrack.jetbrains.com/issue/KT-47936
     */
    dependsOn("compileKotlin")

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
