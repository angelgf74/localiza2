package com.localiza2.ui.contacts

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.localiza2.api.RetrofitClient
import com.localiza2.databinding.BottomSheetPairContactBinding
import com.localiza2.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PairContactBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPairContactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetPairContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        val api = RetrofitClient.create(sessionManager)

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Código QR"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Por email"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> { binding.panelQr.visibility = View.VISIBLE; binding.panelEmail.visibility = View.GONE }
                    1 -> { binding.panelQr.visibility = View.GONE; binding.panelEmail.visibility = View.VISIBLE }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Cargar QR al abrir
        loadQr(api)

        binding.btnQrRefresh.setOnClickListener { loadQr(api) }

        binding.btnSendInvite.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val alias = binding.etAlias.text?.toString()?.trim() ?: ""
            if (email.isBlank() || alias.isBlank()) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendInvite(api, email, alias)
        }
    }

    private fun loadQr(api: com.localiza2.api.ApiService) {
        binding.imgQr.visibility = View.GONE
        binding.tvQrError.visibility = View.GONE
        binding.progressQr.visibility = View.VISIBLE
        binding.tvQrExpiry.text = ""

        lifecycleScope.launch {
            runCatching { api.generatePairingQr() }
                .onSuccess { resp ->
                    val pairUrl = buildPairUrl(resp.token)
                    val bitmap = withContext(Dispatchers.Default) { generateQrBitmap(pairUrl, 512) }
                    binding.progressQr.visibility = View.GONE
                    if (bitmap != null) {
                        binding.imgQr.setImageBitmap(bitmap)
                        binding.imgQr.visibility = View.VISIBLE
                        val expDate = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(resp.expiresAtMillis))
                        binding.tvQrExpiry.text = "Válido hasta $expDate"
                    } else {
                        showQrError("No se pudo generar el código")
                    }
                }
                .onFailure { err ->
                    binding.progressQr.visibility = View.GONE
                    showQrError(err.message ?: "Error desconocido")
                }
        }
    }

    private fun sendInvite(api: com.localiza2.api.ApiService, email: String, alias: String) {
        binding.btnSendInvite.isEnabled = false
        binding.tvEmailResult.visibility = View.GONE

        lifecycleScope.launch {
            runCatching { api.inviteContact(com.localiza2.models.InviteContactDto(email, alias)) }
                .onSuccess {
                    binding.tvEmailResult.text = "Invitación enviada a $email"
                    binding.tvEmailResult.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
                    binding.tvEmailResult.visibility = View.VISIBLE
                    binding.etEmail.text?.clear()
                    binding.etAlias.text?.clear()
                }
                .onFailure { err ->
                    binding.tvEmailResult.text = err.message ?: "Error al enviar"
                    binding.tvEmailResult.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                    binding.tvEmailResult.visibility = View.VISIBLE
                }
            binding.btnSendInvite.isEnabled = true
        }
    }

    private fun showQrError(msg: String) {
        binding.tvQrError.text = msg
        binding.tvQrError.visibility = View.VISIBLE
    }

    private fun buildPairUrl(token: String): String {
        return "https://localiza2-app.angelgf.com.es/pair.html?token=$token"
    }

    private fun generateQrBitmap(content: String, size: Int): Bitmap? = runCatching {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also { bmp ->
            for (x in 0 until size) for (y in 0 until size) {
                bmp.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
            }
        }
    }.getOrNull()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        fun newInstance() = PairContactBottomSheet()
    }
}
