package com.localiza2.ui.map

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class HistoryRequest(val contactId: Int, val alias: String)

class MapSharedViewModel : ViewModel() {
    private val _historyRequest = MutableStateFlow<HistoryRequest?>(null)
    val historyRequest: StateFlow<HistoryRequest?> = _historyRequest

    fun requestHistory(contactId: Int, alias: String) {
        _historyRequest.value = HistoryRequest(contactId, alias)
    }

    fun clearHistory() {
        _historyRequest.value = null
    }
}
