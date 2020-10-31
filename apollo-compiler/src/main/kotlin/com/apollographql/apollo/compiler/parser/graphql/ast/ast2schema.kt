import com.apollographql.apollo.compiler.parser.graphql.ast.GQLDocument
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema

fun GQLDocument.toIntrospectionSchema() {
  return IntrospectionSchema(
    queryType = rootOperationTypeName("query")
  )
}

private fun GQLDocument.rootOperationTypeName(operationType: String): String? {
  TODO("Not yet implemented")
}
