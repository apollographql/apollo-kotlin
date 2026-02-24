package test

import com.apollographql.apollo.api.json.MapJsonReader
import com.apollographql.apollo.api.parseData
import com.apollographql.apollo.api.parseResponse
import com.apollographql.apollo.ast.ParserOptions
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.execution.ExecutableSchema
import com.apollographql.apollo.execution.GraphQLRequest
import java.io.File
import kotlin.test.Test
import fragment.arguments.GetFooQuery
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals

class FragmentArguementsTest {
  @Test
  fun fragmentArgumentsEndToEnd() {
    val parserOptions = ParserOptions.Builder().allowFragmentArguments(true).build()
    val executableSchema = ExecutableSchema.Builder()
        .schema(File("src/main/graphql/schema.graphqls").toGQLDocument())
        .parserOptions(parserOptions)
        .resolver {
          42
        }
        .build()

    val query = GetFooQuery()

    val result = runBlocking {
      executableSchema.execute(GraphQLRequest.Builder().document(query.document()).build())
    }

    val reader = MapJsonReader(result.data)
    val data = query.parseData(reader)

    assertEquals(42, data?.queryDetails?.foo)
  }
}
