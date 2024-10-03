package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.navigation.findFragmentSpreads
import com.apollographql.ijplugin.util.rawType
import com.apollographql.ijplugin.util.resolve
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jsgraphql.psi.GraphQLArgument
import com.intellij.lang.jsgraphql.psi.GraphQLArrayValue
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLInputObjectTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLInputValueDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLNonNullType
import com.intellij.lang.jsgraphql.psi.GraphQLNullValue
import com.intellij.lang.jsgraphql.psi.GraphQLObjectField
import com.intellij.lang.jsgraphql.psi.GraphQLObjectValue
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLVariable
import com.intellij.lang.jsgraphql.psi.GraphQLVariableDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.parentOfType

class ApolloOneOfGraphQLViolationInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : GraphQLVisitor() {
      override fun visitObjectValue(o: GraphQLObjectValue) {
        super.visitObjectValue(o)
        val parent = if (o.parent is GraphQLArrayValue) o.parent.parent else o.parent
        val inputValueDefinition: GraphQLInputValueDefinition = (parent as? GraphQLArgument ?: parent as? GraphQLObjectField)
            ?.resolve()
            ?: return
        val graphQLTypeName = inputValueDefinition.type?.rawType ?: return
        val graphQLInputObjectTypeDefinition: GraphQLInputObjectTypeDefinition = graphQLTypeName.resolve() ?: return
        val isOneOf = graphQLInputObjectTypeDefinition.directives.any { it.name == "oneOf" }
        if (!isOneOf) return

        if (o.objectFieldList.size != 1) {
          holder.registerProblem(o, ApolloBundle.message("inspection.oneOfGraphQLViolation.reportText.oneFieldMustBeSupplied", graphQLTypeName.name!!))
        } else {
          val field = o.objectFieldList.first()
          when (val value = field.value) {
            is GraphQLNullValue -> {
              holder.registerProblem(o, ApolloBundle.message("inspection.oneOfGraphQLViolation.reportText.fieldMustNotBeNull", field.name!!, graphQLTypeName.name!!))
            }

            is GraphQLVariable -> {
              // Look for the parent operation - if there isn't one, we're in a fragment: search for an operation using this fragment
              val operationDefinition = field.parentOfType<GraphQLTypedOperationDefinition>()
                  ?: field.parentOfType<GraphQLFragmentDefinition>()?.let { fragmentParent ->
                    findFragmentSpreads(fragmentParent.project) { it.nameIdentifier.reference?.resolve() == fragmentParent.nameIdentifier }.firstOrNull()
                        ?.parentOfType<GraphQLTypedOperationDefinition>()
                  }
                  ?: return
              val variableDefinition: GraphQLVariableDefinition = operationDefinition.variableDefinitions
                  ?.variableDefinitions?.firstOrNull { it.variable.name == value.name }
                  ?: return
              if (variableDefinition.type !is GraphQLNonNullType) {
                holder.registerProblem(
                    o,
                    ApolloBundle.message(
                        "inspection.oneOfGraphQLViolation.reportText.variableMustBeNonNullType",
                        value.name!!,
                        variableDefinition.type?.text ?: "Unknown"
                    )
                )
              }
            }
          }
        }
      }
    }
  }
}
