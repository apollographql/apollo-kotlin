package com.apollographql.android.gradle

import org.gradle.api.Project;

class ApolloPluginTestHelper {

  private static def setupAndroidProject(Project project) {
    def localProperties = new File("${project.projectDir.absolutePath}", "local.properties")
    localProperties.write("sdk.dir=${androidHome()}")

    def manifest = new File("${project.projectDir.absolutePath}/src/main", "AndroidManifest.xml")
    manifest.getParentFile().mkdirs()
    manifest.createNewFile()
    manifest.write("<manifest package=\"com.example.apollographql\"/>")
    project.apply plugin: 'com.android.application'
  }

  static def setupDefaultAndroidProject(Project project) {
    setupAndroidProject(project)
    project.android {
      compileSdkVersion 25
      buildToolsVersion "25.0.2"
    }
  }

  static def setupAndroidProjectWithProductFlavours(Project project) {
    setupAndroidProject(project)
    project.android {
      compileSdkVersion 25
      buildToolsVersion "25.0.2"
      productFlavors {
        demo {
          applicationIdSuffix ".demo"
          versionNameSuffix "-demo"
        }
        full {
          applicationIdSuffix ".full"
          versionNameSuffix "-full"
        }
      }
    }
  }

  static def applyApolloPlugin(Project project) {
    project.apply plugin: 'com.apollographql.android'
  }

  static def androidHome() {
    def envVar = System.getenv("ANDROID_HOME")
    if (envVar) {
      return envVar
    }
    File localPropFile = new File(new File(System.getProperty("user.dir")).parentFile, "local.properties")
    if (localPropFile.isFile()) {
      Properties props = new Properties()
      props.load(new FileInputStream(localPropFile))
      def sdkDir = props.getProperty("sdk.dir")
      if (sdkDir) {
        return sdkDir
      }
      throw IllegalStateException(
          "SDK location not found. Define location with sdk.dir in the local.properties file or " +
              "with an ANDROID_HOME environment variable.")
    }
  }

}
