package com.apollographql.android.gradle.integration

import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*

fun prepareLocalProperties(destDir: File) {
  File(destDir, "local.properties").writeText("sdk.dir=${androidHome()}\n")
}

fun prepareProjectTestDir(destDir: File, srcDir: File) {
  FileUtils.copyDirectory(srcDir, destDir)
  prepareLocalProperties(destDir)
}

fun createTempTestDirectory(fixtureName: String) : File {
  val dir = File(System.getProperty("user.dir"), "build/inegrationTests/$fixtureName")
  FileUtils.deleteDirectory(dir)
  FileUtils.forceMkdir(dir)
  return dir
}

fun androidHome(): String {
  val envVar = System.getenv("ANDROID_HOME")
  if (envVar != null) {
    return envVar
  }
  val localPropFile = File(File(System.getProperty("user.dir")).parentFile, "local.properties")
  if (localPropFile.isFile()) {
    val props = Properties()
    localPropFile.inputStream().use {
      props.load(it)
    }
    val sdkDir = props.getProperty("sdk.dir")
    if (sdkDir != null) {
      return sdkDir
    }
  }
  throw IllegalStateException("SDK location not found. Define location with sdk.dir in the local.properties file or " +
      "with an ANDROID_HOME environment variable.")
}
