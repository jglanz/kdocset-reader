package com.github.jglanz.kdocset.idea.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@State(name = "KDocsetSettings", storages = [Storage("kdocset-reader.xml")])
class KDocsetSettingsState : PersistentStateComponent<KDocsetSettingsState.StateBean> {
  data class StateBean(
    var docsetsParentPath: String? = null
  )

  private var state = StateBean()

  override fun getState(): StateBean = state

  override fun loadState(state: StateBean) {
    this.state = state
  }

  var docsetsParentPath: String?
    get() = state.docsetsParentPath
    set(value) { state.docsetsParentPath = value }

  companion object {
    fun getInstance(): KDocsetSettingsState = service()
  }
}
