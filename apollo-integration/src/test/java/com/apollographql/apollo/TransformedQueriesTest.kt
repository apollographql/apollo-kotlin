package com.apollographql.apollo

import com.apollographql.apollo.integration.httpcache.AllFilmsQuery
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class TransformedQueriesTest {
  @Test
  fun transformedQueriesMatchTheModels() {
    val transformedQuery = File("build/generated/transformedQueries/apollo/debug/httpcache/com/apollographql/apollo/integration/httpcache/AllFilms.graphql")
    assertThat(AllFilmsQuery.builder().build().queryDocument()).isEqualTo(transformedQuery.readText())
  }
}