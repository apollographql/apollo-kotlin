package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.unified.codegen.CgImplementation
import com.apollographql.apollo3.compiler.unified.codegen.CgInterface
import com.apollographql.apollo3.compiler.unified.codegen.CgModelType
import com.apollographql.apollo3.compiler.unified.codegen.CgProperty
import com.apollographql.apollo3.compiler.unified.codegen.ModelPath
import com.apollographql.apollo3.compiler.unified.codegen.PathElement
import com.apollographql.apollo3.compiler.unified.codegen.toCg


internal fun createInterfaces(rootField: IrField, path: ModelPath): List<CgInterface> {
  return rootField.toCgInterfaces(path)
}

internal fun createImplementations(
    rootField: IrField,
    path: ModelPath,
    allFragmentFieldSets: Map<String, CgInterface>,
): List<CgInterface> {
  return rootField.toCgImplementations(path, allFragmentFieldSets)
}

/**
 * @param the path leading (but not including) to this field
 */
private fun IrField.toCgInterfaces(path: ModelPath): List<CgInterface> {
  return fieldSets
      .filter { it.possibleTypes.isEmpty() }
      .map { fieldSet ->
        fieldSet.toCgInterface(path, this)
      }
}

/**
 * @param the path leading (but not including) to this field
 */
private fun IrField.toCgProperty(path: ModelPath, override: Boolean): CgProperty {
  val compoundType = when (type) {
    is IrObjectType,
    is IrInterfaceType,
    is IrUnionType,
    -> CgModelType(path + PathElement(type.toTypeSet(), responseName))
    else -> null
  }

  return CgProperty(
      name = responseName,
      description = description,
      deprecationReason = deprecationReason,
      type = type.toCg(compoundType),
      override = override,
  )
}

private fun IrFieldSet.toCgInterface(path: ModelPath, field: IrField): CgInterface {
  @Suppress("NAME_SHADOWING")
  val path = path + PathElement(typeSet, field.responseName)

  return CgInterface(
      path = path,
      description = field.description,
      deprecationReason = field.deprecationReason,
      properties = fields.map {
        it.toCgProperty(path, false)
      },
      nestedModels = fields.flatMap {
        it.toCgInterfaces(path)
      }
  )
}

private fun createCgImplementation(path: ModelPath, field: IrField, irFieldSet: IrFieldSet): CgInterface {
  @Suppress("NAME_SHADOWING")
  val path = path + PathElement(irFieldSet.typeSet, field.responseName)

  return CgInterface(
      path = path,
      description = field.description,
      deprecationReason = field.deprecationReason,
      properties = irFieldSet.fields.map {
        it.toCgProperty(path, false)
      },
      nestedModels = fields.flatMap {
        it.toCgInterfaces(path)
      }
  )
}
private class CollectedField(val irField: IrField, val typeSet: TypeSet)
/**
 * @param the path leading (but not including) to this field
 * @param relatedInterfaces all the interfaces
 */
private fun IrField.toCgImplementations(
    path: ModelPath,
    allFragmentFieldSets: Map<String, List<IrFieldSet>>,
): List<CgImplementation> {
  return shapesFieldSets
      .map { shapeFieldSet ->
        val superFieldSets = interfaceFieldSets.filter {
          shapeFieldSet.typeSet.implements(it.typeSet)
        } + allFragmentFieldSets.filter {
          shapeFieldSet.namedFragments.contains(it.key)
        }.values.flatten()

        val collectedFields = (superFieldSets + superFragmentFieldSets).flatMap { fieldSet ->
          fieldSet.fields.map { CollectedField(it, fieldSet.typeSet) }
        }

        val nakedImplementation = shapeFieldSet.toCgImplementation(path, this, allFragmentFieldSets)
        superFieldSets.fold(nakedImplementation) { implementation, superFieldSet ->
          implementation.addSuperInterface(path, responseName, superFieldSet)
        }
      }
}

/**
 *
 */
private fun CgImplementation.addSuperInterface(path: ModelPath, responseName: String, fieldSet: IrFieldSet): CgImplementation {
  val fieldSetType = CgModelType(path + PathElement(fieldSet.typeSet, responseName))
  val nestedModels = nestedModels.map { implementation ->
    implementation.path.elements.last().responseName
  }
  return this.copy(
      implements = implements + fieldSetType,
      nestedModels = nestedModels.

  )
}

private fun IrFieldSet.toCgImplementation(
    path: ModelPath,
    field: IrField,
    allFragmentFieldSets: Map<String, List<IrFieldSet>>,
): CgImplementation {
  @Suppress("NAME_SHADOWING")
  val path = path + PathElement(typeSet, field.responseName)

  val properties = mutableListOf<CgProperty>()
  val nestedModels = mutableListOf<CgImplementation>()
  fields.forEach { childField ->
    val relatedProperties = superInterfaces.mapNotNull {
      it.properties.firstOrNull {
        it.name == childField.responseName
      }
    }
    val relatedInterfaces = superInterfaces.mapNotNull {
      it.nestedModels.firstOrNull {
        it.path.elements.last().responseName == childField.responseName
      }
    }
    val override = relatedProperties.isNotEmpty()

    properties.add(childField.toCgProperty(path, override))
    nestedModels.addAll(childField.toCgImplementations(path, relatedInterfaces, allFragmentFieldSets))
  }

  return CgImplementation(
      path = path,
      description = field.description,
      deprecationReason = field.deprecationReason,
      properties = properties,
      nestedModels = nestedModels,
      implements = superInterfaces.map { it. }
  )
}

private fun IrType.toTypeSet() = setOf(leafName)
