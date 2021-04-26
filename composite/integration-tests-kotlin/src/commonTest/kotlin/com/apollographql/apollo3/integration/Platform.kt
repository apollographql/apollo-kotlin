package com.apollographql.apollo3.integration

/**
 * reads a file in the testFixtures/ folder
 */
expect fun readTestFixture(name: String): String

expect fun checkTestFixture(actualText: String, name: String)
