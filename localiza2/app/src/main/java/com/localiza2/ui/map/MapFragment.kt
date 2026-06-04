package com.localiza2.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.localiza2.R
import com.localiza2.api.RetrofitClient
import com.localiza2.databinding.FragmentMapBinding
import com.localiza2.models.ContactLocationDto
import com.localiza2.utils.SessionManager
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.time.Duration
import java.time.Instant

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var map: MapView
    private lateinit var viewModel: MapViewModel
    private val markers = mutableMapOf<Int, Marker>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sessionManager = SessionManager(requireContext())
        viewModel = MapViewModel(RetrofitClient.create(sessionManager))

        map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(5.0)
        map.controller.setCenter(GeoPoint(40.416775, -3.703790))

        try {
            val myLocation = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), map)
            myLocation.enableMyLocation()
            map.overlays.add(myLocation)
        } catch (_: SecurityException) {}

        binding.chipAll.setOnClickListener { viewModel.loadAllLocations() }

        lifecycleScope.launch {
            viewModel.locations.collect { locations -> updateMap(locations) }
        }
        lifecycleScope.launch {
            viewModel.contactChips.collect { contacts ->
                binding.chipGroup.removeAllViews()
                binding.chipGroup.addView(binding.chipAll)
                contacts.forEach { contact ->
                    val chip = Chip(requireContext()).apply {
                        text = contact.alias
                        isCheckable = true
                        setOnClickListener { viewModel.loadContactLocation(contact.id) }
                    }
                    binding.chipGroup.addView(chip)
                }
            }
        }

        viewModel.loadAllLocations()
    }

    private fun freshnessColor(timestampStr: String): Int {
        val ageMin = try {
            Duration.between(Instant.parse(timestampStr), Instant.now()).toMinutes()
        } catch (_: Exception) { Long.MAX_VALUE }
        return when {
            ageMin < 5  -> Color.parseColor("#4CAF50")
            ageMin < 30 -> Color.parseColor("#FFC107")
            else        -> Color.parseColor("#9E9E9E")
        }
    }

    private fun createMarkerBitmap(color: Int): BitmapDrawable {
        val size = (36 * resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = (2 * resources.displayMetrics.density)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, borderPaint)
        return BitmapDrawable(resources, bitmap)
    }

    private fun updateMap(locations: List<ContactLocationDto>) {
        map.overlays.removeAll(markers.values.toSet())
        markers.clear()

        val geoPoints = mutableListOf<GeoPoint>()

        locations.forEach { loc ->
            val position = GeoPoint(loc.latitude, loc.longitude)
            val color = freshnessColor(loc.timestamp)
            val marker = Marker(map).apply {
                this.position = position
                title = loc.alias
                icon = createMarkerBitmap(color)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { _, _ ->
                    showContactInfo(loc)
                    true
                }
            }
            markers[loc.contactId] = marker
            map.overlays.add(marker)
            geoPoints.add(position)
        }

        if (geoPoints.size > 1) {
            map.zoomToBoundingBox(BoundingBox.fromGeoPoints(geoPoints), true, 100)
        } else if (geoPoints.size == 1) {
            map.controller.setZoom(15.0)
            map.controller.setCenter(geoPoints.first())
        }

        map.invalidate()
    }

    private fun showContactInfo(location: ContactLocationDto) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_contact_info, null)
        view.findViewById<android.widget.TextView>(R.id.tvContactName).text = location.alias
        val ageMin = try {
            Duration.between(Instant.parse(location.timestamp), Instant.now()).toMinutes()
        } catch (_: Exception) { -1L }
        val ageText = when {
            ageMin < 0   -> location.timestamp
            ageMin < 1   -> "Hace menos de 1 minuto"
            ageMin < 60  -> "Hace $ageMin min"
            else         -> "Hace ${ageMin / 60} h"
        }
        val batteryText = location.batteryLevel?.let { " · Batería: $it%" } ?: ""
        view.findViewById<android.widget.TextView>(R.id.tvLastSeen).text = "$ageText$batteryText"
        if (location.photoUrl != null) {
            Glide.with(this)
                .load(location.photoUrl)
                .circleCrop()
                .into(view.findViewById(R.id.ivContactPhoto))
        }
        dialog.setContentView(view)
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
