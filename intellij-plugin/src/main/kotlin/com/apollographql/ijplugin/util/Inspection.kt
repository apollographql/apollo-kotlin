package com.apollographql.ijplugin.util

import com.intellij.codeInspection.ProblemDescriptor

fun ProblemDescriptor.isPreviewMode() = psiElement.containingFile.isPhysical
