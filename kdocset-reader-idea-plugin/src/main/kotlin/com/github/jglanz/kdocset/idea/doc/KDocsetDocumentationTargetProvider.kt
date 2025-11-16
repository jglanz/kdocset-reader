package com.github.jglanz.kdocset.idea.doc

import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement

class KDocsetDocumentationTargetProvider : PsiDocumentationTargetProvider {
  override fun documentationTargets(element: PsiElement, originalElement: PsiElement?): List<DocumentationTarget> {
    val target = KDocsetDocumentationProvider.fromElement(element) ?: return emptyList()
    return listOf(target)
  }
}
