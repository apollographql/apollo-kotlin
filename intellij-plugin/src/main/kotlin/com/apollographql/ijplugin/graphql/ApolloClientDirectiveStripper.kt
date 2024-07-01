@file:JvmName("ApolloClientDirectiveStripper")

package com.apollographql.ijplugin.graphql

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.ast.GQLDirective
import com.apollographql.apollo.ast.TransformResult
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.toUtf8
import com.apollographql.apollo.ast.transform
import com.apollographql.apollo.ast.validateAsSchemaAndAddApolloDefinition
import com.apollographql.ijplugin.util.logw
import com.apollographql.ijplugin.util.schemaFiles
import com.intellij.lang.jsgraphql.psi.GraphQLFile
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager

@OptIn(ApolloInternal::class, ApolloExperimental::class)
fun stripApolloClientDirectives(operationEditor: Editor, operationText: String): String {
  val operationGraphQLFile = operationEditor.project?.let { project -> PsiDocumentManager.getInstance(project).getPsiFile(operationEditor.document) } as? GraphQLFile
      ?: return operationText
  val schemaFiles = operationGraphQLFile.schemaFiles()
  val schemaText = schemaFiles.fold("") { acc, schemaFile -> acc + "\n" + schemaFile.text }
  return runCatching {
    val schemaGQLDocument = schemaText.parseAsGQLDocument().getOrThrow()
    val schema = schemaGQLDocument.validateAsSchemaAndAddApolloDefinition().getOrThrow()
    val operationDocument = operationText.parseAsGQLDocument().getOrThrow()
    val strippedOperationDocument = operationDocument.transform {
      if (it is GQLDirective && schema.shouldStrip(it.name)) {
        TransformResult.Delete
      } else {
        TransformResult.Continue
      }
    }!!
    strippedOperationDocument.toUtf8()
  }
      .onFailure { logw(it) }
      .getOrDefault(operationText)
}
