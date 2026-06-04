package com.localiza2.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localiza2.api.ApiService
import com.localiza2.models.ContactDto
import com.localiza2.models.ContactLocationDto
import com.localiza2.models.InviteContactDto
import com.localiza2.models.UpdateContactDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContactsViewModel(private val api: ApiService) : ViewModel() {

    private val _contacts = MutableStateFlow<List<ContactDto>>(emptyList())
    val contacts: StateFlow<List<ContactDto>> = _contacts

    private val _locations = MutableStateFlow<List<ContactLocationDto>>(emptyList())
    val locations: StateFlow<List<ContactLocationDto>> = _locations

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadContacts() {
        viewModelScope.launch {
            runCatching { api.getContacts() }
                .onSuccess { if (it.isSuccessful) _contacts.value = it.body() ?: emptyList() }
                .onFailure { _error.value = "Error al cargar contactos: ${it.message}" }
        }
        viewModelScope.launch {
            runCatching { api.getContactsLocations() }
                .onSuccess { if (it.isSuccessful) _locations.value = it.body() ?: emptyList() }
        }
    }

    fun inviteContact(email: String, alias: String) {
        viewModelScope.launch {
            runCatching { api.inviteContact(InviteContactDto(email, alias)) }
                .onSuccess { if (it.isSuccessful) loadContacts() else _error.value = "Error al invitar" }
                .onFailure { _error.value = "Error: ${it.message}" }
        }
    }

    fun updateContact(id: Int, alias: String, photoUrl: String?) {
        viewModelScope.launch {
            runCatching { api.updateContact(id, UpdateContactDto(alias, photoUrl)) }
                .onSuccess { loadContacts() }
                .onFailure { _error.value = "Error: ${it.message}" }
        }
    }

    fun deleteContact(id: Int) {
        viewModelScope.launch {
            runCatching { api.deleteContact(id) }
                .onSuccess { loadContacts() }
                .onFailure { _error.value = "Error: ${it.message}" }
        }
    }
}
