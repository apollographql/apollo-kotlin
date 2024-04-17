package hooks

import com.apollographql.apollo3.compiler.CodegenSchema
import com.apollographql.apollo3.compiler.ApolloCompilerPlugin
import com.apollographql.apollo3.compiler.codegen.SchemaAndOperationsLayout

class TestPlugin : ApolloCompilerPlugin {
  private val prefix: String = "GQL"

  override fun layout(codegenSchema: CodegenSchema): SchemaAndOperationsLayout {
    val delegate = SchemaAndOperationsLayout(
        codegenSchema = codegenSchema,
        packageName = "hooks.prefixnames.kotlin",
        rootPackageName = null,
        useSemanticNaming = null,
        decapitalizeFields = null,
        generatedSchemaName = null
    )

    return object : SchemaAndOperationsLayout by delegate {
      
      override fun schemaTypeName(schemaTypeName: String): String {
        return delegate.schemaTypeName(schemaTypeName).prefixed()
      }

      override fun schemaName(): String {
        return delegate.schemaName().prefixed()
      }

      override fun assertionsName(): String {
        return delegate.assertionsName().prefixed()
      }

      override fun paginationName(): String {
        return delegate.paginationName().prefixed()
      }

      override fun operationName(name: String, capitalizedOperationType: String): String {
        return delegate.operationName(name, capitalizedOperationType).prefixed()
      }

      override fun fragmentName(name: String): String {
        return delegate.fragmentName(name).prefixed()
      }

      private fun String.prefixed(): String {
        return "${prefix}${this}"
      }
    }
  }
}
