package com.apollographql.ijplugin.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

fun Project.findPsiFilesByName(fileName: String, searchScope: GlobalSearchScope): List<PsiFile> {
  return FilenameIndex.getVirtualFilesByName(fileName, searchScope).mapNotNull {
    PsiManager.getInstance(this).findFile(it)
  }
}
