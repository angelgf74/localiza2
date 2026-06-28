package com.localiza2.ui.contacts

import android.graphics.Color
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.localiza2.databinding.ItemContactBinding
import com.localiza2.models.ContactDto
import com.localiza2.models.ContactLocationDto
import java.time.Instant
import java.time.Duration

data class ContactWithLocation(
    val contact: ContactDto,
    val location: ContactLocationDto? = null,
    val distanceMeters: Float? = null,
    val isSelf: Boolean = false
)

class ContactsAdapter(
    private val onDelete: (ContactDto) -> Unit,
    private val onEdit: (ContactDto) -> Unit,
    private val onHistory: (ContactDto) -> Unit
) : ListAdapter<ContactWithLocation, ContactsAdapter.ContactViewHolder>(DIFF) {

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ContactWithLocation) {
            val contact = item.contact
            val loc = item.location

            binding.tvAlias.text = contact.alias

            val ageMin: Long? = loc?.let {
                try { Duration.between(Instant.parse(it.timestamp), Instant.now()).toMinutes() }
                catch (_: Exception) { null }
            }

            if (item.isSelf) {
                binding.tvEmail.visibility = View.GONE
                binding.tvStatus.text = if (ageMin != null) formatAge(ageMin) else "Mi posición"
                binding.btnEdit.visibility = View.GONE
                binding.btnDelete.visibility = View.GONE
            } else {
                binding.tvEmail.visibility = View.VISIBLE
                binding.tvEmail.text = contact.email
                binding.tvStatus.text = when (contact.status) {
                    "Accepted" -> if (ageMin != null) formatAge(ageMin) else "Sin ubicación"
                    "Pending"  -> "Pendiente"
                    else       -> "Rechazado"
                }
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnDelete.visibility = View.VISIBLE
                binding.btnEdit.setOnClickListener { onEdit(contact) }
                binding.btnDelete.setOnClickListener { onDelete(contact) }
            }

            if (contact.photoUrl != null) {
                Glide.with(binding.root).load(contact.photoUrl).circleCrop().into(binding.ivPhoto)
            }

            if (loc != null && (item.isSelf || contact.status == "Accepted")) {
                val dotColor = when {
                    ageMin == null || ageMin >= 30 -> Color.parseColor("#9E9E9E")
                    ageMin < 5                     -> Color.parseColor("#4CAF50")
                    else                           -> Color.parseColor("#FFC107")
                }
                binding.viewFreshness.background.setTint(dotColor)
                binding.viewFreshness.visibility = View.VISIBLE

                if (!item.isSelf) {
                    item.distanceMeters?.let { dist ->
                        binding.tvDistance.text = formatDistance(dist)
                        binding.tvDistance.visibility = View.VISIBLE
                    } ?: run { binding.tvDistance.visibility = View.GONE }
                } else {
                    binding.tvDistance.visibility = View.GONE
                }

                loc.batteryLevel?.let { battery ->
                    binding.tvBattery.text = "🔋 $battery%"
                    binding.tvBattery.visibility = View.VISIBLE
                    binding.tvBattery.setTextColor(when {
                        battery <= 15 -> Color.parseColor("#EF4444")
                        battery <= 30 -> Color.parseColor("#F97316")
                        else          -> Color.parseColor("#9CA3AF")
                    })
                } ?: run { binding.tvBattery.visibility = View.GONE }

                binding.btnHistory.visibility = View.VISIBLE
                binding.btnHistory.setOnClickListener { onHistory(contact) }
            } else {
                binding.viewFreshness.visibility = View.INVISIBLE
                binding.tvDistance.visibility = View.GONE
                binding.tvBattery.visibility = View.GONE
                binding.btnHistory.visibility = View.GONE
            }
        }

        private fun formatAge(ageMin: Long): String = when {
            ageMin < 1    -> "ahora mismo"
            ageMin < 60   -> "hace ${ageMin} min"
            ageMin < 1440 -> "hace ${"%.0f".format(ageMin / 60.0)} h"
            else          -> "hace más de 1 día"
        }

        private fun formatDistance(meters: Float): String = when {
            meters < 1000 -> "${meters.toInt()} m"
            else          -> "${"%.1f".format(meters / 1000)} km"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ContactViewHolder(ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ContactWithLocation>() {
            override fun areItemsTheSame(old: ContactWithLocation, new: ContactWithLocation) =
                old.contact.id == new.contact.id
            override fun areContentsTheSame(old: ContactWithLocation, new: ContactWithLocation) =
                old == new
        }
    }
}
