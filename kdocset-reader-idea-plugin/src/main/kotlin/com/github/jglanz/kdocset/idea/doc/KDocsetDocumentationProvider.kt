package com.github.jglanz.kdocset.idea.doc

import com.github.jglanz.kdocset.idea.settings.KDocsetSettingsState
import com.github.jglanz.kdocset.reader.ZDocsetReaderManager
import com.intellij.model.Pointer
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer

/**
 * DocumentationTarget that offers a link to matching Dash/Zeal docset content for a PSI element.
 */
@Suppress("UnstableApiUsage")
class KDocsetDocumentationProvider(
  val symbolText: String,
  val project: Project,
  private val elementPtr: SmartPsiElementPointer<PsiElement>
) : DocumentationTarget {
  
  override fun createPointer(): Pointer<out DocumentationTarget> = Pointer.hardPointer(this)

  override fun computePresentation(): TargetPresentation {
    val name = elementPtr.element?.text?.takeIf { it.isNotBlank() } ?: "Docset Link"
    return TargetPresentation.builder(name).presentation()
  }

  override fun computeDocumentation(): DocumentationResult? {
    elementPtr.element ?: return null
    
    val settings = KDocsetSettingsState.getInstance()
    val folder = settings.docsetsParentPath ?: return null

    return try {
      val mgr = ZDocsetReaderManager(folder)
      for (docset in mgr.list()) {
        val reader = mgr.get(docset)
        val hit = reader.search(symbolText, 1).firstOrNull()
        if (hit != null) {
          val url = hit.fileUrl
          val html = """
            <html><body>
            <div>Docset: $docset</div>
            <div><a href=\"$url\">Open in Docset: ${hit.name}</a></div>
            </body></html>
          """.trimIndent()
          return DocumentationResult.documentation(html)
        }
      }
      null
    } catch (_: Throwable) {
      null
    }
  }

  companion object {
    fun fromElement(element: PsiElement): KDocsetDocumentationProvider? {
      val text = arrayOf(element.text,element.toString()).first { it != null && it.isNotBlank() } ?: return null
      val project = element.project
      val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(element)
      return KDocsetDocumentationProvider(text, project, pointer)
    }
  }
}
