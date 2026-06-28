package com.localiza2.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.localiza2.R
import com.localiza2.api.RetrofitClient
import com.localiza2.databinding.FragmentMapBinding
import com.localiza2.models.ContactLocationDto
import com.localiza2.models.LocationHistoryPointDto
import com.localiza2.utils.SessionManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.time.Duration
import java.time.Instant

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var map: MapView
    private lateinit var viewModel: MapViewModel
    private val sharedViewModel: MapSharedViewModel by activityViewModels()
    private val markers = mutableMapOf<Int, Marker>()
    private val historyOverlays = mutableListOf<Overlay>()

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
        map.setOnTouchListener { v, _ -> hideBubble(); v.performClick(); false }

        try {
            val myLocation = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), map)
            myLocation.enableMyLocation()
            map.overlays.add(myLocation)
        } catch (_: SecurityException) {}

        binding.chipAll.setOnClickListener { viewModel.loadAllLocations() }

        binding.btnCloseHistory.setOnClickListener {
            viewModel.clearHistory()
            viewModel.loadAllLocations()
        }

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
        lifecycleScope.launch {
            combine(viewModel.historyAlias, viewModel.historyPoints) { alias, points ->
                alias to points
            }.collect { (alias, points) ->
                when {
                    alias != null && points.isNotEmpty() -> {
                        showHistoryMode(alias)
                        drawHistory(points)
                    }
                    alias != null -> showHistoryMode(alias)
                    else -> hideHistoryMode()
                }
            }
        }
        lifecycleScope.launch {
            viewModel.historyInfo.collect { info ->
                if (_binding != null) binding.tvHistoryInfo.text = info
            }
        }
        lifecycleScope.launch {
            sharedViewModel.historyRequest.collect { request ->
                if (request != null) {
                    viewModel.loadHistory(request.contactId, request.alias)
                    sharedViewModel.clearHistory()
                }
            }
        }

        viewModel.loadAllLocations()
    }

    private fun showHistoryMode(alias: String) {
        binding.chipScrollView.visibility = View.GONE
        binding.historyBar.visibility = View.VISIBLE
        binding.tvHistoryTitle.text = "Ruta de $alias"
        map.overlays.removeAll(markers.values.toSet())
        map.invalidate()
    }

    private fun hideHistoryMode() {
        binding.chipScrollView.visibility = View.VISIBLE
        binding.historyBar.visibility = View.GONE
        hideBubble()
        clearHistoryOverlays()
    }

    private fun drawHistory(points: List<LocationHistoryPointDto>) {
        clearHistoryOverlays()
        hideBubble()
        val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }

        val polyline = Polyline(map).apply {
            setPoints(geoPoints)
            outlinePaint.color = Color.parseColor("#2196F3")
            outlinePaint.strokeWidth = 8f
            infoWindow = null
            setOnClickListener { _, _, _ -> false }
        }
        map.overlays.add(polyline)
        historyOverlays.add(polyline)

        // Un marcador por cada punto con su tiempo relativo
        val now = Instant.now()
        points.forEachIndexed { index, point ->
            val instant = runCatching { Instant.parse(point.timestamp) }.getOrNull() ?: return@forEachIndexed
            val ageMin = Duration.between(instant, now).toMinutes()
            val label = formatAge(ageMin)
            val color = when (index) {
                points.lastIndex -> Color.parseColor("#2196F3") // más reciente: azul
                0                -> Color.parseColor("#4CAF50") // más antiguo: verde
                else             -> Color.parseColor("#9E9E9E") // intermedios: gris
            }
            val dotSize = if (index == 0 || index == points.lastIndex) 28 else 14
            val gp = geoPoints[index]
            val marker = Marker(map).apply {
                position = gp
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createDotDrawable(color, dotSize)
                infoWindow = null
                setOnMarkerClickListener { _, mapView ->
                    val pt = android.graphics.Point()
                    mapView.projection.toPixels(gp, pt)
                    showBubble(pt.x, pt.y, label)
                    true
                }
            }
            map.overlays.add(marker)
            historyOverlays.add(marker)
        }

        if (geoPoints.size > 1) {
            map.zoomToBoundingBox(BoundingBox.fromGeoPoints(geoPoints), false, 60)
        } else if (geoPoints.size == 1) {
            map.controller.setZoom(15.0)
            map.controller.setCenter(geoPoints.first())
        }
        map.invalidate()
    }

    private fun formatAge(ageMin: Long): String = when {
        ageMin < 1    -> "ahora mismo"
        ageMin < 60   -> "hace $ageMin min"
        ageMin < 1440 -> "hace ${"%.0f".format(ageMin / 60.0)} h"
        else          -> "hace más de 1 día"
    }

    private fun showBubble(anchorX: Int, anchorY: Int, text: String) {
        val bubble = binding.tvMarkerBubble
        bubble.text = text
        bubble.visibility = View.VISIBLE
        bubble.post {
            val dp = resources.displayMetrics.density
            val lp = bubble.layoutParams as FrameLayout.LayoutParams
            var x = anchorX - bubble.width / 2
            var y = anchorY - bubble.height - (10 * dp).toInt()
            x = x.coerceIn(0, (map.width - bubble.width).coerceAtLeast(0))
            if (y < 0) y = anchorY + (10 * dp).toInt()
            lp.leftMargin = x
            lp.topMargin = y
            bubble.layoutParams = lp
        }
    }

    private fun hideBubble() {
        if (_binding != null) binding.tvMarkerBubble.visibility = View.GONE
    }

    private fun clearHistoryOverlays() {
        map.overlays.removeAll(historyOverlays.toSet())
        historyOverlays.clear()
        map.invalidate()
    }

    private fun updateMap(locations: List<ContactLocationDto>) {
        if (viewModel.historyAlias.value != null) return
        map.overlays.removeAll(markers.values.toSet())
        markers.clear()

        val geoPoints = mutableListOf<GeoPoint>()
        locations.forEach { loc ->
            val position = GeoPoint(loc.latitude, loc.longitude)
            val marker = Marker(map).apply {
                this.position = position
                title = loc.alias
                icon = createMarkerBitmap(freshnessColor(loc.timestamp))
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { _, _ -> showContactInfo(loc); true }
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
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }.also {
            canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, it)
        }
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2 * resources.displayMetrics.density
        }.also {
            canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, it)
        }
        return BitmapDrawable(resources, bitmap)
    }

    private fun createDotDrawable(color: Int, sizeDp: Int = 28): BitmapDrawable {
        val size = (sizeDp * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }.also {
            canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, it)
        }
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2 * resources.displayMetrics.density
        }.also {
            canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, it)
        }
        return BitmapDrawable(resources, bmp)
    }

    private fun showContactInfo(location: ContactLocationDto) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_contact_info, null)
        view.findViewById<android.widget.TextView>(R.id.tvContactName).text = location.alias
        val ageMin = try {
            Duration.between(Instant.parse(location.timestamp), Instant.now()).toMinutes()
        } catch (_: Exception) { -1L }
        val ageText = when {
            ageMin < 0  -> location.timestamp
            ageMin < 1  -> "Hace menos de 1 minuto"
            ageMin < 60 -> "Hace $ageMin min"
            else        -> "Hace ${ageMin / 60} h"
        }
        view.findViewById<android.widget.TextView>(R.id.tvLastSeen).text =
            "$ageText${location.batteryLevel?.let { " · Batería: $it%" } ?: ""}"
        if (location.photoUrl != null) {
            Glide.with(this).load(location.photoUrl).circleCrop()
                .into(view.findViewById(R.id.ivContactPhoto))
        }
        dialog.setContentView(view)
        dialog.show()
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
