package com.github.jglanz.kdocset.idea.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class KDocsetSettingsConfigurable : Configurable {
  private var component: JComponent? = null
  private lateinit var folderField: TextFieldWithBrowseButton

  override fun getDisplayName(): String = "KDocset Reader"

  override fun createComponent(): JComponent {
    val settings = KDocsetSettingsState.getInstance()
    folderField = TextFieldWithBrowseButton()
    val folderListener = TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFolderDescriptor())
    folderField.addBrowseFolderListener(
      folderListener
      // "Select Docsets Parent Folder",
      // null,
      // null,
      // FileChooserDescriptorFactory.createSingleFolderDescriptor()
    )
    folderField.text = settings.docsetsParentPath.orEmpty()

    component = panel {
      row("Docsets parent folder:") {
        cell(folderField)//.growX()
      }
      row("Note:") {
        label("This folder should contain one or more *.docset directories (Dash/Zeal docsets).")
      }
    }
    return component!!
  }

  override fun isModified(): Boolean {
    val settings = KDocsetSettingsState.getInstance()
    return (settings.docsetsParentPath.orEmpty() != folderField.text)
  }

  override fun apply() {
    val settings = KDocsetSettingsState.getInstance()
    settings.docsetsParentPath = folderField.text.ifBlank { null }
  }

  override fun reset() {
    val settings = KDocsetSettingsState.getInstance()
    folderField.text = settings.docsetsParentPath.orEmpty()
  }
}
