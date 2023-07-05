package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.util.capitalizeFirstLetter
import com.apollographql.ijplugin.util.decapitalizeFirstLetter
import com.apollographql.ijplugin.util.findChildrenOfType
import com.intellij.lang.jsgraphql.psi.GraphQLElement
import com.intellij.lang.jsgraphql.psi.GraphQLEnumTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLEnumValue
import com.intellij.lang.jsgraphql.psi.GraphQLField
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentSpread
import com.intellij.lang.jsgraphql.psi.GraphQLInlineFragment
import com.intellij.lang.jsgraphql.psi.GraphQLInputObjectTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLInputValueDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtParameter

fun findKotlinOperationDefinitions(operationDefinition: GraphQLTypedOperationDefinition): List<KtClass> {
  val operationName = operationDefinition.name ?: return emptyList()
  val operationType = operationDefinition.operationType.text.capitalizeFirstLetter()
  val project = operationDefinition.project
  // Search for classes with the operation name suffix (the default)
  return findKotlinClass(project, operationName + operationType)
      // Fallback to no suffix
      .ifEmpty {
        findKotlinClass(project, operationName)
      }
      // Discard any classes with the same name but not generated by Apollo
      .filter { it.isApolloOperation() }
}

fun findKotlinFieldDefinitions(graphQLField: GraphQLField): List<PsiElement> {
  return findKotlinClassOfParent(graphQLField)
      ?.flatMap { psiClass ->
        psiClass.findChildrenOfType<KtParameter> { it.name == graphQLField.name }
      }
      ?: emptyList()
}

fun findKotlinFragmentSpreadDefinitions(graphQLFragmentSpread: GraphQLFragmentSpread): List<PsiElement> {
  return findKotlinClassOfParent(graphQLFragmentSpread)
      ?.flatMap { psiClass ->
        psiClass.findChildrenOfType<KtParameter> { it.name == graphQLFragmentSpread.name?.decapitalizeFirstLetter() }
      }
      ?: emptyList()
}

fun findKotlinInlineFragmentDefinitions(graphQLFragmentSpread: GraphQLInlineFragment): List<PsiElement> {
  return findKotlinClassOfParent(graphQLFragmentSpread)
      ?.flatMap { psiClass ->
        psiClass.findChildrenOfType<KtParameter> { it.name == graphQLFragmentSpread.typeCondition?.typeName?.name?.capitalizeFirstLetter()?.let { "on$it" } }
      }
      ?: emptyList()
}

private fun findKotlinClassOfParent(gqlElement: GraphQLElement): List<KtClass>? {
  // TODO We can disambiguate fields with the same name by using the path to the field
  // Try operation first
  return gqlElement.parentOfType<GraphQLTypedOperationDefinition>()?.let { operationDefinition ->
    findKotlinOperationDefinitions(operationDefinition)
  }
  // Fallback to fragment
      ?: gqlElement.parentOfType<GraphQLFragmentDefinition>()?.let { fragmentDefinition ->
        findKotlinFragmentClassDefinitions(fragmentDefinition)
      }
}


fun findKotlinFragmentClassDefinitions(fragmentSpread: GraphQLFragmentSpread): List<KtClass> {
  val fragmentName = fragmentSpread.nameIdentifier.referenceName ?: return emptyList()
  return findKotlinClass(fragmentSpread.project, fragmentName) { it.isApolloFragment() }
}

fun findKotlinFragmentClassDefinitions(fragmentDefinition: GraphQLFragmentDefinition): List<KtClass> {
  val fragmentName = fragmentDefinition.nameIdentifier?.referenceName ?: return emptyList()
  return findKotlinClass(fragmentDefinition.project, fragmentName) { it.isApolloFragment() }
}

fun findKotlinEnumClassDefinitions(enumTypeDefinition: GraphQLEnumTypeDefinition): List<KtClass> {
  val enumName = enumTypeDefinition.typeNameDefinition?.nameIdentifier?.referenceName ?: return emptyList()
  return findKotlinClass(enumTypeDefinition.project, enumName) { it.isApolloEnumClass() }
}

fun findKotlinEnumValueDefinitions(enumValue: GraphQLEnumValue): List<PsiElement> {
  val enumTypeDefinition = enumValue.parentOfType<GraphQLEnumTypeDefinition>() ?: return emptyList()
  return findKotlinEnumClassDefinitions(enumTypeDefinition).flatMap { psiClass ->
    psiClass.findChildrenOfType<KtEnumEntry> { it.name == enumValue.name }
  }
}

fun findKotlinInputClassDefinitions(inputTypeDefinition: GraphQLInputObjectTypeDefinition): List<KtClass> {
  val inputName = inputTypeDefinition.typeNameDefinition?.nameIdentifier?.referenceName ?: return emptyList()
  return findKotlinClass(inputTypeDefinition.project, inputName) { it.isApolloInputClass() }
}

fun findKotlinInputFieldDefinitions(inputValue: GraphQLInputValueDefinition): List<PsiElement> {
  val inputTypeDefinition = inputValue.parentOfType<GraphQLInputObjectTypeDefinition>() ?: return emptyList()
  return findKotlinInputClassDefinitions(inputTypeDefinition).flatMap { psiClass ->
    psiClass.findChildrenOfType<KtParameter> { it.name == inputValue.name }
  }
}

private fun findKotlinClass(project: Project, name: String, filter: ((KtClass) -> Boolean)? = null): List<KtClass> {
  return PsiShortNamesCache.getInstance(project).getClassesByName(
      // All Apollo generated classes ara capitalized
      name.capitalizeFirstLetter(),
      GlobalSearchScope.allScope(project)
  )
      .mapNotNull {
        it.ktClassOrigin
      }
      .let { if (filter != null) it.filter(filter) else it }
}

private val PsiClass.ktClassOrigin get() = (this as? KtUltraLightClass)?.kotlinOrigin as? KtClass
