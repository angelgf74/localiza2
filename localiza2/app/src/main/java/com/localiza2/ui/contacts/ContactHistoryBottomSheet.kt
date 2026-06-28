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
import java.time.Duration
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
            setOnTouchListener { v, _ -> hideBubble(); v.performClick(); false }
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

    private fun showBubble(anchorX: Int, anchorY: Int, text: String) {
        val bubble = binding.tvMarkerBubble
        bubble.text = text
        bubble.visibility = View.VISIBLE
        bubble.post {
            val dp = resources.displayMetrics.density
            val lp = bubble.layoutParams as android.widget.FrameLayout.LayoutParams
            var x = anchorX - bubble.width / 2
            var y = anchorY - bubble.height - (10 * dp).toInt()
            // Mantener dentro de los límites del mapa
            x = x.coerceIn(0, (binding.mapHistory.width - bubble.width).coerceAtLeast(0))
            if (y < 0) y = anchorY + (10 * dp).toInt()
            lp.leftMargin = x
            lp.topMargin = y
            bubble.layoutParams = lp
        }
    }

    private fun hideBubble() {
        binding.tvMarkerBubble.visibility = View.GONE
    }

    private fun drawRoute(points: List<LocationHistoryPointDto>) {
        val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }
        val map = binding.mapHistory
        map.overlays.clear()

        // Polyline (sin click para evitar popup vacío)
        val polyline = Polyline(map).apply {
            setPoints(geoPoints)
            outlinePaint.color = Color.parseColor("#2196F3")
            outlinePaint.strokeWidth = 8f
            setOnClickListener { _, _, _ -> false }
        }
        map.overlays.add(polyline)

        // Marcador por cada punto con tiempo relativo
        val now = Instant.now()
        points.forEachIndexed { index, point ->
            val instant = runCatching { Instant.parse(point.timestamp) }.getOrNull() ?: return@forEachIndexed
            val ageMin = Duration.between(instant, now).toMinutes()
            val label = formatAge(ageMin)
            val color = when (index) {
                points.lastIndex -> Color.parseColor("#2196F3") // más reciente: azul
                0 -> Color.parseColor("#4CAF50")               // más antiguo: verde
                else -> Color.parseColor("#9E9E9E")            // intermedios: gris
            }
            val dotSize = if (index == 0 || index == points.lastIndex) 28 else 14
            val marker = Marker(map).apply {
                position = geoPoints[index]
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createDotDrawable(color, dotSize)
                infoWindow = null
                setOnMarkerClickListener { _, mapView ->
                    val pt = android.graphics.Point()
                    mapView.projection.toPixels(geoPoints[index], pt)
                    showBubble(pt.x, pt.y, label)
                    true
                }
            }
            map.overlays.add(marker)
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

    private fun formatAge(ageMin: Long): String = when {
        ageMin < 1    -> "ahora mismo"
        ageMin < 60   -> "hace $ageMin min"
        ageMin < 1440 -> "hace ${"%.0f".format(ageMin / 60.0)} h"
        else          -> "hace más de 1 día"
    }

    private fun createDotDrawable(color: Int, sizeDp: Int = 28): android.graphics.drawable.BitmapDrawable {
        val size = (sizeDp * resources.displayMetrics.density).toInt()
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
