package com.localiza2.ui.contacts

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.localiza2.api.RetrofitClient
import com.localiza2.databinding.BottomSheetContactHistoryBinding
import com.localiza2.models.LocationHistoryPointDto
import com.localiza2.utils.SessionManager
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ContactHistoryBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetContactHistoryBinding? = null
    private val binding get() = _binding!!

    private val contactId get() = requireArguments().getInt(ARG_CONTACT_ID)
    private val contactAlias get() = requireArguments().getString(ARG_ALIAS, "Contacto")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        _binding = BottomSheetContactHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        binding.tvHistoryTitle.text = "Ruta de $contactAlias"

        binding.mapHistory.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
        }

        binding.btnLoadMore.setOnClickListener {
            val oldest = allPoints.firstOrNull()?.timestamp ?: return@setOnClickListener
            binding.btnLoadMore.isEnabled = false
            binding.btnLoadMore.text = "Cargando…"
            lifecycleScope.launch {
                loadHistorySuspend(before = oldest)
                binding.btnLoadMore.isEnabled = true
                binding.btnLoadMore.text = "Cargar más"
            }
        }

        loadHistory()
    }

    private val allPoints = mutableListOf<LocationHistoryPointDto>()

    private fun loadHistory(before: String? = null) {
        lifecycleScope.launch { loadHistorySuspend(before) }
    }

    private suspend fun loadHistorySuspend(before: String? = null) {
        val api = RetrofitClient.create(SessionManager(requireContext()))
        if (before == null) binding.tvHistoryInfo.text = "Cargando…"
        runCatching { api.getContactLocationHistory(contactId, before = before) }
            .onSuccess { resp ->
                val points = resp.body()
                if (points.isNullOrEmpty()) {
                    if (before == null) binding.tvHistoryInfo.text = "Sin historial disponible"
                } else {
                    if (before == null) allPoints.clear()
                    allPoints.addAll(0, points)
                    drawRoute(allPoints)
                    binding.btnLoadMore.visibility =
                        if (points.size == 50) android.view.View.VISIBLE else android.view.View.GONE
                }
            }
            .onFailure {
                binding.tvHistoryInfo.text = "Error al cargar el historial"
            }
    }

    private fun drawRoute(points: List<LocationHistoryPointDto>) {
        val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }
        val map = binding.mapHistory
        map.overlays.clear()

        // Polyline
        val polyline = Polyline(map).apply {
            setPoints(geoPoints)
            outlinePaint.color = Color.parseColor("#2196F3")
            outlinePaint.strokeWidth = 8f
        }
        map.overlays.add(polyline)

        // Start marker (green)
        if (geoPoints.isNotEmpty()) {
            val startMarker = Marker(map).apply {
                position = geoPoints.first()
                title = "Inicio"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createDotDrawable(Color.parseColor("#4CAF50"))
            }
            map.overlays.add(startMarker)
        }

        // Latest marker (blue)
        if (geoPoints.size > 1) {
            val endMarker = Marker(map).apply {
                position = geoPoints.last()
                title = "Última posición"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createDotDrawable(Color.parseColor("#2196F3"))
            }
            map.overlays.add(endMarker)
        }

        // Fit map to route
        if (geoPoints.size > 1) {
            map.zoomToBoundingBox(BoundingBox.fromGeoPoints(geoPoints), false, 60)
        } else {
            map.controller.setZoom(15.0)
            map.controller.setCenter(geoPoints.first())
        }
        map.invalidate()

        // Info text
        val fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault())
        val first = fmt.format(Instant.parse(points.first().timestamp))
        val last  = fmt.format(Instant.parse(points.last().timestamp))
        binding.tvHistoryInfo.text = "${points.size} posiciones  ·  $first → $last"
    }

    private fun createDotDrawable(color: Int): android.graphics.drawable.BitmapDrawable {
        val size = (28 * resources.displayMetrics.density).toInt()
        val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)
        val border = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2 * resources.displayMetrics.density
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, border)
        return android.graphics.drawable.BitmapDrawable(resources, bmp)
    }

    override fun onResume() {
        super.onResume()
        binding.mapHistory.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapHistory.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CONTACT_ID = "contactId"
        private const val ARG_ALIAS      = "alias"

        fun newInstance(contactId: Int, alias: String) = ContactHistoryBottomSheet().apply {
            arguments = Bundle().apply {
                putInt(ARG_CONTACT_ID, contactId)
                putString(ARG_ALIAS, alias)
            }
        }
    }
}
