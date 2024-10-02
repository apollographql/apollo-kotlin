package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.util.apollo3
import com.apollographql.ijplugin.util.apollo4
import com.apollographql.ijplugin.util.asKtClass
import com.apollographql.ijplugin.util.capitalizeFirstLetter
import com.apollographql.ijplugin.util.className
import com.apollographql.ijplugin.util.containingKtFile
import com.apollographql.ijplugin.util.findChildrenOfType
import com.apollographql.ijplugin.util.resolveKtName
import com.apollographql.ijplugin.util.shortName
import com.apollographql.ijplugin.util.typeArgumentClassName
import com.intellij.lang.jsgraphql.GraphQLFileType
import com.intellij.lang.jsgraphql.psi.GraphQLDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLElement
import com.intellij.lang.jsgraphql.psi.GraphQLEnumTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLEnumValue
import com.intellij.lang.jsgraphql.psi.GraphQLField
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentSpread
import com.intellij.lang.jsgraphql.psi.GraphQLInlineFragment
import com.intellij.lang.jsgraphql.psi.GraphQLInputObjectTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLInputValueDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLOperationDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLRecursiveVisitor
import com.intellij.lang.jsgraphql.psi.GraphQLSelectionSet
import com.intellij.lang.jsgraphql.psi.GraphQLTypeNameDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.descendantsOfType
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.psiUtil.containingClass

private val APOLLO_OPERATION_TYPES = setOf(
    FqName("$apollo3.api.Query"),
    FqName("$apollo3.api.Mutation"),
    FqName("$apollo3.api.Subscription"),

    FqName("$apollo4.api.Query"),
    FqName("$apollo4.api.Mutation"),
    FqName("$apollo4.api.Subscription"),
)

private val APOLLO_FRAGMENT_TYPE = setOf(
    FqName("$apollo3.api.Fragment.Data"),
    FqName("$apollo4.api.Fragment.Data"),
)

private val APOLLO_ENUM_TYPE = setOf(
    "$apollo3.api.EnumType",
    "$apollo4.api.EnumType",
)

fun KtNameReferenceExpression.isApolloOperationOrFragmentReference(): Boolean {
  return resolveKtName()?.asKtClass()?.isApolloOperationOrFragment() == true
}

fun KtNameReferenceExpression.isApolloModelFieldReference(): Boolean {
  val resolved = resolveKtName()
  // Parameter is for data classes, property is for interfaces
  return (resolved is KtParameter || resolved is KtProperty) &&
      (resolved as KtElement).isApolloModelField()
}

fun KtElement.isApolloModelField() = topMostContainingClass()
    ?.isApolloOperationOrFragment() == true


fun KtClass.isApolloOperation(): Boolean {
  return superTypeListEntries.any {
    val superType = it.typeAsUserType?.referenceExpression?.resolveKtName()?.kotlinFqName
    superType in APOLLO_OPERATION_TYPES
  }
}

fun KtClass.isApolloFragment(): Boolean {
  return superTypeListEntries.any {
    val superType = it.typeAsUserType?.referenceExpression?.resolveKtName()?.kotlinFqName
    superType in APOLLO_FRAGMENT_TYPE
  } ||
      // Fallback for fragments in responseBased codegen: they are interfaces generated in a .fragment package.
      // This can lead to false positives, but consequences are not dire.
      isInterface() && kotlinFqName?.parent()?.shortName == "fragment" && hasGeneratedByApolloComment()
}

fun KtClass.isApolloOperationOrFragment(): Boolean {
  return superTypeListEntries.any {
    val superType = it.typeAsUserType?.referenceExpression?.resolveKtName()?.kotlinFqName
    superType in APOLLO_OPERATION_TYPES || superType in APOLLO_FRAGMENT_TYPE
  } ||
      // Fallback for fragments in responseBased codegen: they are interfaces generated in a .fragment package.
      // This can lead to false positives, but consequences are not dire.
      isInterface() && kotlinFqName?.parent()?.shortName == "fragment" && hasGeneratedByApolloComment()
}

fun KtNameReferenceExpression.isApolloEnumClassReference(): Boolean {
  val ktClass = resolveKtName() as? KtClass ?: return false
  return ktClass.isApolloEnumClass()
}

fun KtClass.isApolloEnumClass() = isEnum() &&
    // Apollo enums have a companion object that has a property named "type" of type EnumType
    isEnum() && companionObjects.any { companion ->
  companion.declarations.filterIsInstance<KtProperty>().any { property ->
    property.name == "type" &&
        property.className() in APOLLO_ENUM_TYPE
  }
}

fun KtNameReferenceExpression.isApolloEnumValueReference(): Boolean {
  val ktEnumEntry = resolveKtName() as? KtEnumEntry ?: return false
  return ktEnumEntry.containingClass()?.isApolloEnumClass() == true
}

fun KtNameReferenceExpression.isApolloInputClassReference(): Boolean {
  return resolveKtName()?.asKtClass()?.isApolloInputClass() == true
}

fun KtClass.isApolloInputClass(): Boolean {
  // Apollo input classes are data classes, generated in a package named "type", and we also look at the header comment.
  // This can lead to false positives, but consequences are not dire.
  return isData() &&
      kotlinFqName?.parent()?.shortName == "type" &&
      hasGeneratedByApolloComment()
}

fun KtNameReferenceExpression.isApolloInputFieldReference(): Boolean {
  val resolved = resolveKtName()
  // Parameter is for data classes, property is for interfaces
  return (resolved is KtParameter || resolved is KtProperty) &&
      (resolved as KtElement).topMostContainingClass()
          ?.isApolloInputClass() == true
}

private fun KtElement.hasGeneratedByApolloComment() =
  containingKtFile()?.descendantsOfType<PsiComment>()?.any { it.text.contains("generated by Apollo GraphQL") } == true

private fun KtElement.topMostContainingClass(): KtClass? {
  return if (containingClass() == null) {
    this as? KtClass
  } else {
    containingClass()!!.topMostContainingClass()
  }
}

private fun findGraphQLDefinitions(project: Project, predicate: (GraphQLDefinition) -> Boolean): List<GraphQLDefinition> {
  return FileTypeIndex.getFiles(GraphQLFileType.INSTANCE, GlobalSearchScope.allScope(project)).flatMap { virtualFile ->
    PsiManager.getInstance(project).findFile(virtualFile)
        ?.findChildrenOfType<GraphQLDefinition>()
        ?.filter { predicate(it) }
        ?: emptyList()
  }
}

fun findOperationOrFragmentGraphQLDefinitions(project: Project, name: String): List<GraphQLDefinition> {
  return findGraphQLDefinitions(project) { graphQLDefinition ->
    // Look for operation definitions
    graphQLDefinition is GraphQLOperationDefinition && (graphQLDefinition.name?.capitalizeFirstLetter() == name || graphQLDefinition.name?.capitalizeFirstLetter() == name.minusOperationTypeSuffix()) ||
        // Fallback: look for fragment definitions
        graphQLDefinition is GraphQLFragmentDefinition && graphQLDefinition.name?.capitalizeFirstLetter() == name
  }
}

fun findEnumTypeGraphQLDefinitions(project: Project, name: String): List<GraphQLTypeNameDefinition> {
  return findGraphQLDefinitions(project) {
    it is GraphQLEnumTypeDefinition && it.typeNameDefinition?.name?.capitalizeFirstLetter() == name.capitalizeFirstLetter()
  }
      .mapNotNull { (it as GraphQLEnumTypeDefinition).typeNameDefinition }
}

fun findEnumValueGraphQLDefinitions(nameReferenceExpression: KtNameReferenceExpression): List<GraphQLEnumValue> {
  val project = nameReferenceExpression.project
  val resolved = nameReferenceExpression.resolveKtName()
  val ktEnumEntry = resolved as? KtEnumEntry ?: return emptyList()
  // First argument (rawValue) of the super call is the original enum value name
  val enumValueName = (ktEnumEntry.initializerList?.initializers?.first() as? KtSuperTypeCallEntry)
      ?.valueArgumentList?.arguments?.first()
      ?.stringTemplateExpression?.entries?.first()?.text
  val enumTypeName = ktEnumEntry.containingClass()?.name ?: return emptyList()
  val enumTypeGqlDefinitions = findEnumTypeGraphQLDefinitions(project, enumTypeName)
  return enumTypeGqlDefinitions.flatMap { enumTypeDefinition ->
    (enumTypeDefinition.parent as GraphQLEnumTypeDefinition)
        .enumValueDefinitions?.enumValueDefinitionList
        ?.map { it.enumValue }
        ?.filter {
          it.name == enumValueName
        }
        ?: emptyList()
  }
}

fun findInputTypeGraphQLDefinitions(project: Project, name: String): List<GraphQLTypeNameDefinition> {
  return findGraphQLDefinitions(project) {
    it is GraphQLInputObjectTypeDefinition && it.typeNameDefinition?.name?.capitalizeFirstLetter() == name
  }
      .mapNotNull { (it as GraphQLInputObjectTypeDefinition).typeNameDefinition }
}

fun findInputFieldGraphQLDefinitions(nameReferenceExpression: KtNameReferenceExpression): List<GraphQLInputValueDefinition> {
  val project = nameReferenceExpression.project
  val resolved = nameReferenceExpression.resolveKtName()
  val ktElement = if (resolved is KtParameter || resolved is KtProperty) resolved as KtElement else return emptyList()
  val inputClassName = ktElement.containingClass()?.name ?: return emptyList()
  return findInputTypeGraphQLDefinitions(project, inputClassName).flatMap {
    val inputObjectTypeDefinition = it.parent as GraphQLInputObjectTypeDefinition
    inputObjectTypeDefinition.inputObjectValueDefinitions?.inputValueDefinitionList?.filter {
      it.name == ktElement.name
    } ?: emptyList()
  }
}

fun findGraphQLElements(nameReferenceExpression: KtNameReferenceExpression): List<GraphQLElement> {
  val resolved = nameReferenceExpression.resolveKtName()
  // Parameter is for data classes, property is for interfaces
  val ktElement = if (resolved is KtParameter || resolved is KtProperty) resolved as KtElement else return emptyList()
  return findGraphQLElements(ktElement)
}

/**
 * 'Element' here means either a field, a fragment spread, or an inline fragment.
 */
fun findGraphQLElements(ktElement: KtElement): List<GraphQLElement> {
  val elements = mutableListOf<GraphQLElement>()
  val operationOrFragmentClass = ktElement.topMostContainingClass() ?: return emptyList()
  val fieldPath = operationOrFragmentClass.elementPath(ktElement)
  val operationOrFragmentDefinitions = findOperationOrFragmentGraphQLDefinitions(ktElement.project, operationOrFragmentClass.name!!)
  for (operationOrFragmentDefinition in operationOrFragmentDefinitions) {
    val elementsAtPath = operationOrFragmentDefinition.findElementAtPath(fieldPath)
    if (elementsAtPath != null) {
      elements += elementsAtPath
    } else {
      // 'Best effort' fallback for responseBased codegen: all fields with the name
      elements += operationOrFragmentDefinition.findElementsNamed(ktElement.name!!)
    }
  }
  return elements
}


/**
 * For a given [element], return its path in the given operation class.
 *
 * For instance if the operation class is:
 * ```
 * class MyQuery : Query {
 *   data class Data(val user: User)
 *                              ↖
 *                  data class User(val address: Address)
 *                                                ↖
 *                                    data class Address(val street: String)
 * }
 * ```
 *
 * the path for `Address.street` will be `["user", "address", "street"]`.
 */
private fun KtClass.elementPath(element: KtElement): List<String> {
  var modelClass = element.containingClass()!!
  val parameterPath = mutableListOf(element.name!!)
  while (true) {
    var found = false
    findClass@ for (candidateModelClass in findChildrenOfType<KtClass>(withSelf = true)) {
      // Look for the parameter in the constructor (for data classes) and in the properties (for interfaces)
      for (property in candidateModelClass.primaryConstructor?.valueParameters.orEmpty() + candidateModelClass.getProperties()) {
        if (property.className() == modelClass.fqName?.asString() ||
            // For lists
            property.typeArgumentClassName(0) == modelClass.fqName?.asString()
        ) {
          parameterPath.add(0, (property as PsiNamedElement).name!!)
          modelClass = candidateModelClass
          found = true
          break@findClass
        }
      }
    }
    if (!found) break
  }
  return parameterPath
}

/**
 * Get the element (field, fragment spread, or inline fragment type condition) at the given path.
 */
private fun GraphQLDefinition.findElementAtPath(path: List<String>): GraphQLElement? {
  var element: GraphQLElement? = null
  var selectionSet = findChildrenOfType<GraphQLSelectionSet>(recursive = false).firstOrNull() ?: return null
  for (pathElement in path) {
    var found = false
    for (selection in selectionSet.selectionList) {
      if (selection.field != null) {
        // Field
        val field = selection.field!!
        if (field.name == pathElement ||
            field.alias?.identifier?.referenceName == pathElement
        ) {
          element = field
          field.selectionSet?.let { selectionSet = it }
          found = true
          break
        }
      } else if (selection.fragmentSelection != null) {
        // Fragment
        if (selection.fragmentSelection!!.inlineFragment != null) {
          // Inline
          val inlineFragment = selection.fragmentSelection!!.inlineFragment!!
          if (inlineFragment.typeCondition?.typeName?.name?.let { "on${it.capitalizeFirstLetter()}" } == pathElement) {
            // Point to the type condition
            element = inlineFragment.typeCondition
            inlineFragment.selectionSet?.let { selectionSet = it }
            found = true
            break
          }
        } else if (selection.fragmentSelection!!.fragmentSpread != null) {
          // Spread
          val fragmentSpread = selection.fragmentSelection!!.fragmentSpread!!
          if (fragmentSpread.name.equals(pathElement, ignoreCase = true)) {
            element = fragmentSpread
            found = true
            break
          }
        }
      }
    }
    if (!found) {
      element = null
      break
    }
  }
  return element
}

/**
 * Get all elements (field, fragment spread, or inline fragment type condition) with the given name.
 */
private fun GraphQLDefinition.findElementsNamed(name: String): List<GraphQLElement> {
  // Fields
  return findChildrenOfType<GraphQLField> { field ->
    field.name == name || field.alias?.identifier?.referenceName == name
  } +
      // Fragment spreads
      findChildrenOfType<GraphQLFragmentSpread> { fragmentSpread ->
        fragmentSpread.name.equals(name, ignoreCase = true)
      } +
      // Inline fragments
      findChildrenOfType<GraphQLInlineFragment> { inlineFragment ->
        inlineFragment.typeCondition?.typeName?.name?.let { "on${it.capitalizeFirstLetter()}" } == name
      }
          .map { inlineFragment ->
            // Point to the type condition
            inlineFragment.typeCondition!!
          }
}


// Remove "Query", "Mutation", or "Subscription" from the end of the operation name
private fun String.minusOperationTypeSuffix(): String {
  return when {
    endsWith("Query") -> substringBeforeLast("Query")
    endsWith("Mutation") -> substringBeforeLast("Mutation")
    endsWith("Subscription") -> substringBeforeLast("Subscription")
    else -> this
  }
}

fun findFragmentSpreads(project: Project, predicate: (GraphQLFragmentSpread) -> Boolean): List<GraphQLFragmentSpread> {
  return FileTypeIndex.getFiles(GraphQLFileType.INSTANCE, GlobalSearchScope.allScope(project)).flatMap { virtualFile ->
    val fragmentSpreads = mutableListOf<GraphQLFragmentSpread>()
    val visitor = object : GraphQLRecursiveVisitor() {
      override fun visitFragmentSpread(o: GraphQLFragmentSpread) {
        super.visitFragmentSpread(o)
        if (predicate(o)) {
          fragmentSpreads += o
        }
      }
    }
    PsiManager.getInstance(project).findFile(virtualFile)?.accept(visitor)
    fragmentSpreads
  }
}
