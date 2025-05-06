package hooks

import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.definitionFromScope
import com.apollographql.apollo.ast.rawType
import com.apollographql.apollo.ast.responseName
import com.apollographql.apollo.ast.rootTypeDefinition
import com.apollographql.apollo.compiler.DocumentTransform
import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerPluginProvider

class TestPluginProvider: ApolloCompilerPluginProvider {
  override fun create(environment: ApolloCompilerPluginEnvironment): ApolloCompilerPlugin {
    return TestPlugin()
  }
}

class TestPlugin : ApolloCompilerPlugin {
  override fun documentTransform(): DocumentTransform {
    return object : DocumentTransform {
      override fun transform(
          schema: Schema,
          document: GQLDocument,
          extraFragmentDefinitions: List<GQLFragmentDefinition>,
      ): GQLDocument {
        return document.copy(
            definitions = document.definitions.map {
              when (it) {
                is GQLOperationDefinition -> {
                  it.copy(
                      selections = it.selections.alwaysGreet(schema, it.rootTypeDefinition(schema)!!.name)
                  )
                }
                is GQLFragmentDefinition -> {
                  it.copy(
                      selections = it.selections.alwaysGreet(schema, it.typeCondition.name)
                  )
                }
                else -> it
              }
            }
        )
      }
    }
  }

  private fun List<GQLSelection>.alwaysGreet(schema: Schema, parentType: String): List<GQLSelection> {
    val selections = this.map {
      when (it) {
        is GQLField -> it.copy(
            selections = it.selections.alwaysGreet(schema, it.definitionFromScope(schema, parentType)!!.type.rawType().name)
        )
        is GQLFragmentSpread -> it
        is GQLInlineFragment -> it.copy(
            selections = it.selections.alwaysGreet(schema, it.typeCondition?.name ?: parentType)
        )
      }
    }

    return if (parentType == "Friend" && selections.none { it is GQLField && it.responseName() == "greet" }) {
      selections + GQLField(null, null, "greet", emptyList(), emptyList(), emptyList())
    } else {
      selections
    }
  }
}
