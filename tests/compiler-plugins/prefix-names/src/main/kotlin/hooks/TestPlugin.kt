package hooks

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerRegistry
import com.apollographql.apollo.compiler.codegen.SchemaAndOperationsLayout

class TestPlugin() : ApolloCompilerPlugin {
  override fun beforeCompilationStep(
      environment: ApolloCompilerPluginEnvironment,
      registry: ApolloCompilerRegistry,
  ) {
    val prefix = environment.arguments.get("prefix") as String

    registry.registerLayout { codegenSchema ->
      val delegate = SchemaAndOperationsLayout(
          codegenSchema = codegenSchema,
          packageName = "hooks.prefixnames.kotlin",
          rootPackageName = null,
          useSemanticNaming = null,
          decapitalizeFields = null,
          generatedSchemaName = null
      )

      object : SchemaAndOperationsLayout by delegate {

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
}

