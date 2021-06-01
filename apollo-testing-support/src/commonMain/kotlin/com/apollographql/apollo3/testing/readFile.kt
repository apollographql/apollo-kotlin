package com.apollographql.apollo3.testing

/**
 * reads a file in the testFixtures/ folder
 */
expect fun readFile(path: String): String
expect fun checkFile(actualText: String, path: String)
