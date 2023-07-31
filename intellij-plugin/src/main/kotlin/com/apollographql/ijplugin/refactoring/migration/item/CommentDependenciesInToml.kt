package com.apollographql.ijplugin.refactoring.migration.item

import com.apollographql.ijplugin.util.findPsiFilesByExtension
import com.apollographql.ijplugin.util.unquoted
import com.intellij.codeInspection.SuppressionUtil.createComment
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlPsiFactory
import org.toml.lang.psi.TomlRecursiveVisitor
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.kind

class CommentDependenciesInToml(
    private vararg val artifactId: String,
) : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    val libsVersionTomlFiles: List<PsiFile> = project.findPsiFilesByExtension("versions.toml", searchScope)
    val usages = mutableListOf<MigrationItemUsageInfo>()
    for (file in libsVersionTomlFiles) {
      if (file !is TomlFile) continue
      file.accept(object : TomlRecursiveVisitor() {
        override fun visitLiteral(element: TomlLiteral) {
          super.visitLiteral(element)
          if (element.kind is TomlLiteralKind.String) {
            val dependencyText = element.text.unquoted()
            if (artifactId.any { dependencyText.contains(it) }) {
              var elementToReplace: PsiElement = element
              while (elementToReplace.parent != null && elementToReplace.parent !is TomlTable) {
                elementToReplace = elementToReplace.parent
              }
              usages.add(elementToReplace.toMigrationItemUsageInfo())
            }
          }
        }
      })
    }
    return usages
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element
    element.parent.addBefore(
        createComment(project, " TODO: remove this declaration and its uses in your gradle files", element.language),
        element
    )
    element.parent.addBefore(TomlPsiFactory(project).createWhitespace("\n"), element)
    element.replace(createComment(project, " " + element.text, element.language))
  }
}
