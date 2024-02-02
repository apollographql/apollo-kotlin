package hooks

import com.apollographql.apollo3.compiler.CodegenSchema
import com.apollographql.apollo3.compiler.Plugin
import com.apollographql.apollo3.compiler.codegen.SchemaAndOperationsLayout

class TestPlugin : Plugin {
  private val prefix: String = "GQL"

  override fun layout(codegenSchema: CodegenSchema): SchemaAndOperationsLayout {
    val delegate = SchemaAndOperationsLayout(
        codegenSchema = codegenSchema,
        packageName = "hooks.prefixnames.kotlin",
        rootPackageName = null,
        useSemanticNaming = null,
        decapitalizeFields = null
    )

    return object : SchemaAndOperationsLayout {
      override fun schemaPackageName(): String {
        return delegate.schemaPackageName()
      }

      override fun schemaTypeName(schemaTypeName: String): String {
        return topLevelName(delegate.schemaTypeName(schemaTypeName))
      }

      override fun topLevelName(name: String): String {
        return "${prefix}${name}"
      }

      override fun propertyName(name: String): String {
        return delegate.propertyName(name)
      }

      override fun executableDocumentPackageName(filePath: String?): String {
        return delegate.executableDocumentPackageName(filePath)
      }

      override fun operationName(name: String, capitalizedOperationType: String): String {
        return topLevelName(delegate.operationName(name, capitalizedOperationType))
      }

      override fun fragmentName(name: String): String {
        return topLevelName(delegate.fragmentName(name))
      }

    }
  }
}
