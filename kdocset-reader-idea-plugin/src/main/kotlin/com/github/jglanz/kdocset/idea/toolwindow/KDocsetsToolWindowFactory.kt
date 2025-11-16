package com.github.jglanz.kdocset.idea.toolwindow

import com.github.jglanz.kdocset.reader.ZDocsetReaderManager
import com.github.jglanz.kdocset.reader.ZDocsetSearchResult
import com.github.jglanz.kdocset.idea.settings.KDocsetSettingsState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.addKeyboardAction
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.ListCellRenderer

class KDocsetsToolWindowFactory : ToolWindowFactory, DumbAware {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val panel = SimpleToolWindowPanel(true, true)
    val content = ContentFactory.getInstance().createContent(panel, "", false)
    toolWindow.contentManager.addContent(content)

    panel.setContent(createMainComponent())
  }

  private fun createMainComponent(): JComponent {
    val settings = KDocsetSettingsState.getInstance()
    val parent = settings.docsetsParentPath
    val browser: JBCefBrowser? = if (JBCefApp.isSupported()) JBCefBrowser() else null

    val docsetCombo = JComboBox<String>()
    val reloadBtn = JButton("Reload")
    val searchField = JBTextField()
    val resultsModel = DefaultListModel<ZDocsetSearchResult>()
    val resultsList = JBList(resultsModel).apply {
      cellRenderer = ListCellRenderer { list, value, index, isSelected, cellHasFocus ->
        val displayText = "${value.name} [${value.type}]"
        // defaultRenderer.getListCellRendererComponent(list, displayText, index, isSelected, cellHasFocus)
        panel {
          row { text(displayText) }
        }
        
      }
    }

    fun loadDocsets() {
      val folder = settings.docsetsParentPath
      val names = try {
        if (folder.isNullOrBlank()) emptyList() else ZDocsetReaderManager(folder).list()
      } catch (t: Throwable) {
        emptyList()
      }
      docsetCombo.model = DefaultComboBoxModel(names.toTypedArray())
    }

    fun performSearch() {
      resultsModel.clear()
      val folder = settings.docsetsParentPath
      val selected = docsetCombo.selectedItem as? String ?: return
      val query = searchField.text.trim()
      if (folder.isNullOrBlank() || query.isBlank()) return
      try {
        val reader = ZDocsetReaderManager(folder).get(selected)
        reader.search(query, 50).forEach { resultsModel.addElement(it) }
      } catch (err: Throwable) {
        err.printStackTrace()
      }
    }

    resultsList.addListSelectionListener {
      val item = resultsList.selectedValue ?: return@addListSelectionListener
      val url = item.fileUrl
      browser?.loadURL(url)
    }

    reloadBtn.addActionListener { loadDocsets() }
    searchField.addKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0)) {
      performSearch()
    }

    loadDocsets()

    val leftPanel = JPanel(BorderLayout()).apply {
      add(JBScrollPane(resultsList), BorderLayout.CENTER)
    }

    val rightPanel = JPanel(BorderLayout()).apply {
      if (browser != null) {
        add(browser.component, BorderLayout.CENTER)
      } else {
        add(JBScrollPane(JBList(listOf("JCEF is not available on this platform."))), BorderLayout.CENTER)
      }
    }

    val splitter = JBSplitter(false, 0.3f).apply {
      firstComponent = leftPanel
      secondComponent = rightPanel
      dividerWidth = JBUI.scale(4)
    }

    val topControls = panel {
      row("Docset:") {
        cell(docsetCombo).align(AlignX.FILL).resizableColumn()
        cell(reloadBtn)
      }
      row("Search:") {
        cell(searchField).align(AlignX.FILL).resizableColumn()
      }
    }

    return JPanel(BorderLayout()).apply {
      add(topControls, BorderLayout.NORTH)
      add(splitter, BorderLayout.CENTER)
    }
  }
}
