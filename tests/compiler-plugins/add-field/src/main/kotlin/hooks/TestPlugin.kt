package hooks

import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.definitionFromScope
import com.apollographql.apollo3.ast.rawType
import com.apollographql.apollo3.ast.responseName
import com.apollographql.apollo3.ast.rootTypeDefinition
import com.apollographql.apollo3.compiler.DocumentTransform
import com.apollographql.apollo3.compiler.Plugin

class TestPlugin : Plugin {
  override fun documentTransform(): DocumentTransform {
    return object : DocumentTransform {
      override fun transform(schema: Schema, operation: GQLOperationDefinition): GQLOperationDefinition {
        return operation.copy(
            selections = operation.selections.alwaysGreet(schema, operation.rootTypeDefinition(schema)!!.name)
        )
      }

      override fun transform(schema: Schema, fragment: GQLFragmentDefinition): GQLFragmentDefinition {
        return fragment.copy(
            selections = fragment.selections.alwaysGreet(schema, fragment.typeCondition.name)
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
