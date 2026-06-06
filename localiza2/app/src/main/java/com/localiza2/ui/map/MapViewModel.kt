package com.localiza2.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localiza2.api.ApiService
import com.localiza2.models.ContactDto
import com.localiza2.models.ContactLocationDto
import com.localiza2.models.LocationHistoryPointDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MapViewModel(private val api: ApiService) : ViewModel() {

    private val _locations = MutableStateFlow<List<ContactLocationDto>>(emptyList())
    val locations: StateFlow<List<ContactLocationDto>> = _locations

    private val _contactChips = MutableStateFlow<List<ContactDto>>(emptyList())
    val contactChips: StateFlow<List<ContactDto>> = _contactChips

    private val _historyAlias = MutableStateFlow<String?>(null)
    val historyAlias: StateFlow<String?> = _historyAlias

    private val _historyPoints = MutableStateFlow<List<LocationHistoryPointDto>>(emptyList())
    val historyPoints: StateFlow<List<LocationHistoryPointDto>> = _historyPoints

    private val _historyInfo = MutableStateFlow("")
    val historyInfo: StateFlow<String> = _historyInfo

    init {
        loadContactList()
    }

    private fun loadContactList() {
        viewModelScope.launch {
            runCatching { api.getContacts() }
                .onSuccess { resp ->
                    _contactChips.value = resp.body()
                        ?.filter { it.status == "Accepted" && it.locationPermissionGranted }
                        ?: emptyList()
                }
        }
    }

    fun loadAllLocations() {
        viewModelScope.launch {
            runCatching { api.getContactsLocations() }
                .onSuccess { _locations.value = it.body() ?: emptyList() }
        }
    }

    fun loadContactLocation(contactId: Int) {
        viewModelScope.launch {
            runCatching { api.getContactLocation(contactId) }
                .onSuccess { resp -> resp.body()?.let { _locations.value = listOf(it) } }
        }
    }

    fun loadHistory(contactId: Int, alias: String) {
        viewModelScope.launch {
            _historyAlias.value = alias
            _historyPoints.value = emptyList()
            _historyInfo.value = "Cargando…"
            runCatching { api.getContactLocationHistory(contactId) }
                .onSuccess { resp ->
                    val pts = resp.body()
                    if (pts.isNullOrEmpty()) {
                        _historyInfo.value = "Sin historial disponible"
                    } else {
                        val fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault())
                        val first = fmt.format(Instant.parse(pts.first().timestamp))
                        val last  = fmt.format(Instant.parse(pts.last().timestamp))
                        _historyInfo.value = "${pts.size} posiciones · $first → $last"
                        _historyPoints.value = pts
                    }
                }
                .onFailure {
                    _historyInfo.value = "Error al cargar el historial"
                }
        }
    }

    fun clearHistory() {
        _historyAlias.value = null
        _historyPoints.value = emptyList()
        _historyInfo.value = ""
    }
}
