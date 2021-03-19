package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.unified.codegen.CGAdapter
import com.apollographql.apollo3.compiler.unified.codegen.CGFragmentModelType
import com.apollographql.apollo3.compiler.unified.codegen.CGImplementation
import com.apollographql.apollo3.compiler.unified.codegen.CGInterface
import com.apollographql.apollo3.compiler.unified.codegen.CGModelType
import com.apollographql.apollo3.compiler.unified.codegen.CGProperty
import com.apollographql.apollo3.compiler.unified.codegen.ModelPath
import com.apollographql.apollo3.compiler.unified.codegen.PathElement
import com.apollographql.apollo3.compiler.unified.codegen.toCg

internal data class Result(
    val interfaces: List<CGInterface>,
    val implementations: List<CGImplementation>,
    val adapter: CGAdapter,
)

internal class ModelBuilder(
    val filePath: String,
    val rootField: IrField,
    val path: ModelPath,
) {

  private fun IrType.toTypeSet() = setOf(leafName)

  fun build(): Result {
    // build the model tree without any inheritance information
    val interfaces = rootField.toCGInterfaces(path)

    return Result(
        interfaces = interfaces,
        implementations =
    )
  }

  /**
   * @param the path leading (but not including) to this field
   */
  private fun IrField.toCGInterfaces(path: ModelPath): List<CGInterface> {
    return fieldSets.map { fieldSet ->
      fieldSet.toCGInterface(path, this)
    }
  }

  /**
   * @param the path leading (but not including) to this field
   */
  private fun IrField.toCGProperty(path: ModelPath): CGProperty {
    val compoundType = when(type) {
      is ObjectIrType,
      is InterfaceIrType,
      is UnionIrType -> CGModelType(path + PathElement(type.toTypeSet(), responseName))
      else -> null
    }

    return CGProperty(
        name = responseName,
        description = description,
        deprecationReason = deprecationReason,
        type = type.toCg(compoundType),
        override = false,
    )
  }

  private fun IrFieldSet.toCGInterface(path: ModelPath, field: IrField): CGInterface {
    @Suppress("NAME_SHADOWING")
    val path = path + PathElement(typeSet, field.responseName)

    return CGInterface(
        path = path,
        description = field.description,
        deprecationReason = field.deprecationReason,
        properties = fields.map {
          it.toCGProperty(path)
        },
        nestedModels = fields.flatMap {
          it.toCGInterfaces(path)
        }
    )
  }
}