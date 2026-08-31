package test

import com.apollographql.apollo.api.json.MapJsonReader
import com.apollographql.apollo.api.parseData
import com.apollographql.apollo.ast.ParserOptions
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.execution.ExecutableSchema
import com.apollographql.apollo.execution.GraphQLRequest
import empty.selection.sets.GetNodeQuery
import empty.selection.sets.GetUserQuery
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EmptySelectionSetsTest {
  @Test
  fun emptySelectionSetIsSentToTheServer() {
    assertEquals("query GetUser { user { } }", GetUserQuery().document())
    assertEquals("query GetNode { node { __typename ... on User { } } }", GetNodeQuery().document())
  }

  @Test
  fun emptySelectionSetOnAFieldEndToEnd() {
    val data = execute(GetUserQuery())
    assertNotNull(data.user)
  }

  @Test
  fun emptySelectionSetInAnInlineFragmentEndToEnd() {
    val data = execute(GetNodeQuery())
    assertNotNull(data.node.onUser)
  }

  private fun <D : com.apollographql.apollo.api.Query.Data> execute(query: com.apollographql.apollo.api.Query<D>): D {
    val executableSchema = ExecutableSchema.Builder()
        .schema(File("src/main/graphql/schema.graphqls").toGQLDocument())
        .parserOptions(ParserOptions.Builder().allowEmptySelectionSets(true).build())
        .resolver { mapOf<String, Any?>() }
        .typeResolver { _, _ -> "User" }
        .build()

    val response = runBlocking {
      executableSchema.execute(GraphQLRequest.Builder().document(query.document()).build())
    }
    assertEquals(null, response.errors)

    return query.parseData(MapJsonReader(response.data!!))!!
  }
}
