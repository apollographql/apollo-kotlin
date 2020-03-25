#!/usr/bin/env kotlin
import java.io.File

/**
 * A script to run locally in order to make a release.
 *
 * You need kotlin 1.3.70+ installed on your machine
 */

fun runCommand(vararg args: String): String {
    val builder = ProcessBuilder(*args)
            .redirectError(ProcessBuilder.Redirect.INHERIT)

    val process = builder.start()
    val ret = process.waitFor()

    val output = process.inputStream.bufferedReader().readText()
    if (ret != 0) {
        throw java.lang.Exception("command ${args.joinToString(" ")} failed:\n$output")
    }

    return output
}

fun setCurrentVersion(version: String) {
    val gradleProperties = File("gradle.properties")
    var newContent = gradleProperties.readLines().map {
        it.replace(Regex("VERSION_NAME=.*"), "VERSION_NAME=$version")
    }.joinToString(separator = "\n", postfix = "\n")
    gradleProperties.writeText(newContent)

    val dependencies = File("gradle/dependencies.gradle")
    //    apollo                : '1.2.2', // This should only be used by apollo-gradle-plugin-incubating:test to get the artifacts locally
    newContent = dependencies.readLines().map {
        it.replace(Regex("( *apollo *: *').*('.*)")) {
            "${it.groupValues[1]}$version${it.groupValues[2]}"
        }
    }.joinToString(separator = "\n", postfix = "\n")
    dependencies.writeText(newContent)
}

fun getCurrentVersion(): String {
    val versionLines = File("gradle.properties").readLines().filter { it.startsWith("VERSION_NAME=") }

    require(versionLines.size > 0) {
        "cannot find the version in ./gradle.properties"
    }

    require(versionLines.size == 1) {
        "multiple versions found in ./gradle.properties"
    }

    val regex = Regex("VERSION_NAME=(.*)-SNAPSHOT")
    val matchResult = regex.matchEntire(versionLines.first())

    require(matchResult != null) {
        "'${versionLines.first()}' doesn't match VERSION_NAME=(.*)-SNAPSHOT"
    }

    return matchResult.groupValues[1]
}

fun getNext(version: String, position: Int) = version.split(".").mapIndexed { index, s ->
    when {
        index == position -> (s.toInt() + 1).toString()
        index > position -> "0"
        else -> s
    }
}.joinToString(".")

fun getNextPatch(version: String) = getNext(version, 2)
fun getNextMinor(version: String) = getNext(version, 1)
fun getNextMajor(version: String) = getNext(version, 0)

if (runCommand("git", "status", "--porcelain").isNotEmpty()) {
    println("Your git repo is not clean. Make sur to stash or commit your changes before making a release")
    System.exit(1)
}

val version = getCurrentVersion()
val nextPatch = getNextPatch(version)
val nextMinor = getNextMinor(version)
val nextPatchAfterMinor = getNextPatch(nextMinor)
val nextMajor = getNextMajor(version)
val nextPatchAfterMajor = getNextPatch(nextMajor)

var tagVersion: String = ""

while (tagVersion.isEmpty()) {
    println("Current version is '$version-SNAPSHOT'.")
    println("1. patch: tag $version and bump to $nextPatch")
    println("2. minor: tag $nextMinor and bump to $nextPatchAfterMinor")
    println("3. major: tag $nextMajor and bump to $nextPatchAfterMajor")
    println("What do you want to do [1/2/3]?")

    val answer = readLine()!!.trim()
    when (answer) {
        "1" -> tagVersion = version
        "2" -> tagVersion = nextMinor
        "3" -> tagVersion = nextMajor
    }
}

setCurrentVersion(tagVersion)

runCommand("git", "commit", "-a", "-m", "release $tagVersion")
runCommand("git", "tag", "v$tagVersion")

val snapshot = "${getNextPatch(tagVersion)}-SNAPSHOT"
setCurrentVersion(snapshot)
runCommand("git", "commit", "-a", "-m", "version is now $snapshot")

println("Everything is done. Verify everything is ok and type `git push origin master` to trigger the new version.")
