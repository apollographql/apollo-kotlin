package com.apollostack.compiler

import java.io.File

fun irFileFor(pkg: String, queryFileName: String) =
    File("src/test/graphql/com/example/$pkg/$queryFileName.json")

fun actualFileFor(pkg: String, queryFileName: String) =
    File("build/generated/source/apollo/com/example/$pkg/$queryFileName.java")

fun expectedFileFor(pkg: String, queryFileName: String) =
    File("src/test/graphql/com/example/$pkg/${queryFileName}Expected.java")

