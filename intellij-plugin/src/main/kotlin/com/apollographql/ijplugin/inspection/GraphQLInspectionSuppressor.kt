package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.SuppressionUtil
import com.intellij.lang.jsgraphql.ide.validation.fixes.GraphQLSuppressByCommentFix
import com.intellij.lang.jsgraphql.psi.GraphQLField
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.regex.Pattern

private val SUPPRESS_IN_LINE_COMMENT_PATTERN = Pattern.compile("#" + SuppressionUtil.COMMON_SUPPRESS_REGEXP)

class GraphQLInspectionSuppressor : InspectionSuppressor {
  override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
    return element.isSuppressedOnSelfOrParent(toolId, SUPPRESS_IN_LINE_COMMENT_PATTERN)
  }

  override fun getSuppressActions(
      element: PsiElement?,
      toolId: String,
  ): Array<SuppressQuickFix> {
    return if (element == null) {
      SuppressQuickFix.EMPTY_ARRAY
    } else {
      arrayOf(
          GraphQLSuppressByCommentFix(toolId, GraphQLField::class.java, ApolloBundle.message("inspection.suppress.field")),
      )
    }
  }
}

fun PsiElement.isSuppressedOnSelfOrParent(
    toolId: String,
    suppressInLineCommentPattern: Pattern,
): Boolean {
  var element: PsiElement? = this
  while (element != null) {
    val prev = PsiTreeUtil.skipWhitespacesBackward(element)
    if (prev is PsiComment) {
      val text = prev.getText()
      val matcher = suppressInLineCommentPattern.matcher(text)
      if (matcher.matches() && SuppressionUtil.isInspectionToolIdMentioned(matcher.group(1), toolId)) {
        return true
      }
    }
    element = element.parent
  }
  return false
}
