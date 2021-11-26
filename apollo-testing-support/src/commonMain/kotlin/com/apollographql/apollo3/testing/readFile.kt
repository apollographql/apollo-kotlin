package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloExperimental

/**
 * reads a file in the testFixtures/ folder
 */
@ApolloExperimental
expect fun readFile(path: String): String
@ApolloExperimental
expect fun checkFile(actualText: String, path: String)
