package com.apollographql.ijplugin.refactoring.migration.item

import com.apollographql.ijplugin.util.findPsiFilesByExtension
import com.apollographql.ijplugin.util.quoted
import com.apollographql.ijplugin.util.unquoted
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlInlineTable
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlPsiFactory
import org.toml.lang.psi.TomlRecursiveVisitor
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.kind

class UpdateGradleDependenciesInToml(
    private val oldGroupId: String,
    private val newGroupId: String,
    private val newVersion: String,
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
            if (dependencyText == oldGroupId || dependencyText.startsWith("$oldGroupId:")) {
              usages.add(MigrationItemUsageInfo(this@UpdateGradleDependenciesInToml, element.firstChild, Kind.SHORT_MODULE_OR_GROUP))
              // Find the associated version
              val versionEntry = (element.parent.parent as? TomlInlineTable)?.entries
                  ?.firstOrNull { it.key.text == "version" || it.key.text == "version.ref" }
              if (versionEntry != null) {
                if (versionEntry.key.text == "version") {
                  versionEntry.value?.let {
                    usages.add(
                        MigrationItemUsageInfo(
                            this@UpdateGradleDependenciesInToml,
                            it.firstChild,
                            Kind.VERSION
                        )
                    )
                  }
                } else {
                  // Resolve the reference
                  val versionsTable = element.containingFile.children.filterIsInstance<TomlTable>()
                      .firstOrNull { it.header.key?.text == "versions" }
                  val versionRefKey = versionEntry.value?.text?.unquoted()
                  val refTarget = versionsTable?.entries?.firstOrNull { it.key.text == versionRefKey }
                  refTarget?.value?.let {
                    usages.add(
                        MigrationItemUsageInfo(
                            this@UpdateGradleDependenciesInToml,
                            it.firstChild,
                            Kind.VERSION
                        )
                    )
                  }
                }
              }
            }
          }
        }
      })
    }
    return usages
  }

  private enum class Kind {
    SHORT_MODULE_OR_GROUP, VERSION
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element
    when (usage.attachedData<Kind>()) {
      Kind.SHORT_MODULE_OR_GROUP -> {
        val notation = element.text.unquoted().split(":")
        val newNotation = when (notation.size) {
          1 -> newGroupId
          2 -> {
            val part = notation[1]
            if (part.matches(Regex("\\d+.*"))) {
              // Part is a version number: plugin short notation case, e.g. "com.apollographql.apollo:2.5.14"
              "$newGroupId:$newVersion"
            } else {
              // Part is an artifact name: library case, e.g. "com.apollographql.apollo:apollo-runtime"
              "$newGroupId:$part"
            }
          }

          else -> "$newGroupId:${notation[1]}:$newVersion"
        }
        element.replace(TomlPsiFactory(project).createLiteral(newNotation.quoted()))
      }

      Kind.VERSION -> element.replace(TomlPsiFactory(project).createLiteral(newVersion.quoted()))
    }
  }
}
