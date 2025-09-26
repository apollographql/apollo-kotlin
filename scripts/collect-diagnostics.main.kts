#!/usr/bin/env kotlin

/**
 * Copy this file to scripts/collect-diagnostics.main.kts and add the below to your
 * GitHub Actions workflow file:
 *
 *       - if: always()
 *         run: ./scripts/collect-diagnostics.main.kts
 *       - uses: actions/upload-artifact@65462800fd760344b1a7b4382951275a0abb4808 #v4.3.3
 *         if: always()
 *         with:
 *           name: push.zip
 *           path: diagnostics.zip
 */

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

val home = System.getenv("HOME")
println("HOME: $home")

val coreDir = File("$home/Library/Logs/DiagnosticReports/")

ZipOutputStream(File("diagnostics.zip").outputStream()).use { zipOutputStream ->
  if (coreDir.exists()) {
    zipOutputStream.addDirectory(coreDir, coreDir, "diagnostics/cores")
  }

  File(".").walk().filter {
    it.name == "build"
  }.map {
    it.resolve("reports")
  }.filter {
    it.exists()
  }.forEach {
    zipOutputStream.addDirectory(File("."), it, "diagnostics/tests")
  }
}

fun ZipOutputStream.addDirectory(repoDir: File, reportsDir: File, prefix: String) {
  reportsDir.walk().filter { it.isFile }.forEach {
    val path = it.relativeTo(repoDir).path
    val name = "$prefix/$path"
    println("- $name")

    putNextEntry(ZipEntry(name))
    it.inputStream().use {
      it.copyTo(this)
    }
  }
}
