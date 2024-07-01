package hooks

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerPluginLogger
import com.apollographql.apollo.compiler.ApolloCompilerPluginProvider
import com.apollographql.apollo.compiler.CodegenSchema
import com.apollographql.apollo.compiler.codegen.SchemaAndOperationsLayout

class TestPlugin(
    val prefix: String,
    logger: ApolloCompilerPluginLogger,
) : ApolloCompilerPlugin {
  init {
    logger.info("TestPlugin.prefix=$prefix")
  }

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

class TestPluginProvider : ApolloCompilerPluginProvider {
  override fun create(environment: ApolloCompilerPluginEnvironment): ApolloCompilerPlugin {
    return TestPlugin(environment.arguments.get("prefix") as String, environment.logger)
  }

}
