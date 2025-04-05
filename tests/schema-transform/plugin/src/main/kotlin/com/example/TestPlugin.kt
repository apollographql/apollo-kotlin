package com.example

import com.apollographql.apollo.ast.GQLDefinition
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLFieldDefinition
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.SourceAwareException
import com.apollographql.apollo.ast.toUtf8
import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerPluginProvider
import com.apollographql.apollo.compiler.SchemaTransform

class TestPluginProvider : ApolloCompilerPluginProvider {
  override fun create(environment: ApolloCompilerPluginEnvironment): ApolloCompilerPlugin {
    return TestPlugin()
  }
}

private class TraversalState(
    val originalInterfaces: Map<String, GQLInterfaceTypeDefinition>,
) {
  val visitedInterfaces = mutableMapOf<String, GQLInterfaceTypeDefinition>()
  val definitions = mutableListOf<GQLDefinition>()
}

private fun traverse(definition: GQLDefinition, state: TraversalState) {
  when (definition) {
    is GQLObjectTypeDefinition -> {
      var patchedFields = definition.fields
      definition.implementsInterfaces.forEach {
        var superInterface = state.visitedInterfaces.get(it)
        if (superInterface == null) {
          traverse(state.originalInterfaces.get(it) as GQLInterfaceTypeDefinition, state)
          superInterface = state.visitedInterfaces.get(it)!!
        }

        val fieldsByName = superInterface.fields.associateBy { it.name }
        patchedFields = patchedFields.patch(fieldsByName)
      }
      val patchedDefinition = definition.copy(fields = patchedFields)
      state.definitions.add(patchedDefinition)
    }
    is GQLInterfaceTypeDefinition -> {
      if (state.visitedInterfaces.containsKey(definition.name)) {
        return
      }
      var patchedFields = definition.fields
      definition.implementsInterfaces.forEach {
        var superInterface = state.visitedInterfaces.get(it)
        if (superInterface == null) {
          traverse(state.originalInterfaces.get(it) as GQLInterfaceTypeDefinition, state)
          superInterface = state.visitedInterfaces.get(it)!!
        }

        val fieldsByName = superInterface.fields.associateBy { it.name }
        patchedFields = patchedFields.patch(fieldsByName)
      }
      val patchedDefinition = definition.copy(fields = patchedFields)
      state.visitedInterfaces.put(definition.name, patchedDefinition)
      state.definitions.add(patchedDefinition)
    }
    else -> {
      state.definitions.add(definition)
    }
  }
}

private fun List<GQLFieldDefinition>.patch(superFields: Map<String, GQLFieldDefinition>): List<GQLFieldDefinition> {
  return map { fieldDefinition ->
    val superField = superFields.get(fieldDefinition.name)
    if (superField == null) {
      return@map fieldDefinition
    }

    val superSemanticNonNull = superField.directives.firstOrNull { it.name == "semanticNonNull" }
    if (superSemanticNonNull == null) {
      return@map fieldDefinition
    }

    val selfSemanticNonNull = fieldDefinition.directives.firstOrNull { it.name == "semanticNonNull" }
    if (selfSemanticNonNull == null) {
      return@map fieldDefinition.copy(directives = fieldDefinition.directives + superSemanticNonNull)
    }

    val superStr = superSemanticNonNull.toUtf8()
    val selfStr = selfSemanticNonNull.toUtf8()
    if(superStr != selfStr) {
      throw SourceAwareException("The @semanticNonNull directive doesn't match the super field directive (expected '$superStr')", selfSemanticNonNull.sourceLocation)
    }

    return@map fieldDefinition
  }

}

class TestPlugin : ApolloCompilerPlugin {
  override fun schemaTransform(): SchemaTransform? {
    return object : SchemaTransform {
      override fun transform(schemaDocument: GQLDocument): GQLDocument {
        val state = TraversalState(schemaDocument.definitions.filterIsInstance<GQLInterfaceTypeDefinition>().associateBy { it.name })

        schemaDocument.definitions.forEach {
          traverse(it, state)
        }

        return GQLDocument(state.definitions, null)
      }
    }
  }
}
