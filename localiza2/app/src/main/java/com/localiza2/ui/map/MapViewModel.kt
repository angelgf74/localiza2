package com.localiza2.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localiza2.api.ApiService
import com.localiza2.models.ContactDto
import com.localiza2.models.ContactLocationDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel(private val api: ApiService) : ViewModel() {

    private val _locations = MutableStateFlow<List<ContactLocationDto>>(emptyList())
    val locations: StateFlow<List<ContactLocationDto>> = _locations

    private val _contactChips = MutableStateFlow<List<ContactDto>>(emptyList())
    val contactChips: StateFlow<List<ContactDto>> = _contactChips

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
}
