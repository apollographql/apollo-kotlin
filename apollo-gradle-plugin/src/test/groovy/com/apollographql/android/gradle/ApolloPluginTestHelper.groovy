package com.apollographql.android.gradle

import org.apache.commons.io.FileUtils
import org.gradle.api.Project;

class ApolloPluginTestHelper {
  static def setupJavaProject(Project project) {
    project.apply plugin: 'java'
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

  private static def setupAndroidProject(Project project) {
    def localProperties = new File("${project.projectDir.absolutePath}", "local.properties")
    localProperties.write("sdk.dir=${androidHome()}")

    def manifest = new File("${project.projectDir.absolutePath}/src/main", "AndroidManifest.xml")
    manifest.getParentFile().mkdirs()
    manifest.createNewFile()
    manifest.write("<manifest package=\"com.example.apollographql\"/>")
    project.apply plugin: 'com.android.application'
  }

  public enum ProjectType {
    Android, Java
  }

  static def File createTempTestDirectory(String testProjectName) {
    File dir = new File(System.getProperty("user.dir"), "build/inegrationTests/$testProjectName")
    FileUtils.deleteDirectory(dir)
    FileUtils.forceMkdir(dir)
    return dir
  }

  static void prepareProjectTestDir(File destDir, ProjectType type, String testProjectName, String testBuildScriptName) {
    String testProjectsRoot = "src/test/testProject"

    File projectTypeRoot
    if (type.equals(ProjectType.Android)) {
      projectTypeRoot = new File("$testProjectsRoot/android")
    } else if (type.equals(ProjectType.Java)) {
      projectTypeRoot = new File("$testProjectsRoot/java")
    } else {
      throw new IllegalArgumentException("Not a valid project type")
    }

    File projectUnderTest = new File(System.getProperty("user.dir"), "$projectTypeRoot/$testProjectName")
    if (!projectUnderTest.isDirectory()) {
      throw new IllegalArgumentException("Couldn't find test project")
    }

    File requestedBuildScript = new File("$projectTypeRoot/buildScriptFixtures/${testBuildScriptName}.gradle")
    if (!requestedBuildScript.isFile()) {
      throw new IllegalArgumentException("Couldn't find the test build script")
    }

    prepareLocalProperties(destDir)
    FileUtils.copyDirectory(projectUnderTest, destDir)
    FileUtils.copyFile(requestedBuildScript, new File("$destDir/build.gradle"))
  }

  static def prepareLocalProperties(File destDir) {
    def localProperties = new File(destDir, "local.properties")
    localProperties.write("sdk.dir=${androidHome()}")
  }
}
