package com.localiza2.ui.contacts

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.localiza2.R
import com.localiza2.api.RetrofitClient
import com.localiza2.databinding.FragmentContactsBinding
import com.localiza2.models.ContactDto
import com.localiza2.models.ContactLocationDto
import com.localiza2.utils.SessionManager
import kotlinx.coroutines.launch

class ContactsFragment : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ContactsViewModel
    private lateinit var adapter: ContactsAdapter
    private var deviceLocation: Location? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sessionManager = SessionManager(requireContext())
        viewModel = ContactsViewModel(RetrofitClient.create(sessionManager))

        adapter = ContactsAdapter(
            onDelete = { contact ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Eliminar contacto")
                    .setMessage("¿Eliminar a ${contact.alias}?")
                    .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteContact(contact.id) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onEdit = { contact ->
                val dialogView = layoutInflater.inflate(R.layout.dialog_edit_contact, null)
                val etAlias = dialogView.findViewById<TextInputEditText>(R.id.etAlias)
                etAlias.setText(contact.alias)
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Editar contacto")
                    .setView(dialogView)
                    .setPositiveButton("Guardar") { _, _ ->
                        viewModel.updateContact(contact.id, etAlias.text.toString(), contact.photoUrl)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onHistory = { contact ->
                ContactHistoryBottomSheet.newInstance(contact.id, contact.alias)
                    .show(childFragmentManager, "history")
            }
        )

        binding.recyclerContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerContacts.adapter = adapter

        binding.fabAddContact.setOnClickListener {
            PairContactBottomSheet.newInstance().show(childFragmentManager, "pair")
        }

        lifecycleScope.launch {
            viewModel.contacts.collect { mergeAndSubmit() }
        }
        lifecycleScope.launch {
            viewModel.locations.collect { mergeAndSubmit() }
        }
        lifecycleScope.launch {
            viewModel.error.collect { msg -> msg?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() } }
        }

        viewModel.loadContacts()
        fetchDeviceLocation()
    }

    @SuppressLint("MissingPermission")
    private fun fetchDeviceLocation() {
        if (!hasLocationPermission()) return
        LocationServices.getFusedLocationProviderClient(requireActivity())
            .lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    deviceLocation = loc
                    mergeAndSubmit()
                }
            }
    }

    private fun hasLocationPermission() =
        ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun mergeAndSubmit() {
        val contacts = viewModel.contacts.value
        val locations = viewModel.locations.value

        // contactId=0 is "myself" from API; fall back to device GPS if absent
        val myApiLoc = locations.firstOrNull { it.contactId == 0 }
        val myLat = myApiLoc?.latitude ?: deviceLocation?.latitude
        val myLng = myApiLoc?.longitude ?: deviceLocation?.longitude

        val items = contacts.map { contact ->
            val loc = locations.firstOrNull { it.contactId == contact.id }
            val dist = if (myLat != null && myLng != null && loc != null) {
                val result = FloatArray(1)
                Location.distanceBetween(myLat, myLng, loc.latitude, loc.longitude, result)
                result[0]
            } else null
            ContactWithLocation(contact, loc, dist)
        }
        adapter.submitList(items)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
