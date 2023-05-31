package com.example

import com.apollographql.apollo3.ApolloClient
import com.example.generated.AnimalsQuery

suspend fun main() {
    val apolloClient = ApolloClient.Builder()
        .serverUrl("https://example.com")
        .build()

    val response = apolloClient.query(AnimalsQuery()).execute()
    println(response.data!!.animals[0].name)
}
