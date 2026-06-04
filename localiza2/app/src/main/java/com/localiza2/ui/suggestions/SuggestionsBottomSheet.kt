package com.localiza2.ui.suggestions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.localiza2.api.SuggestionsApiService
import com.localiza2.databinding.BottomSheetSuggestionsBinding
import com.localiza2.models.SuggestionDto
import com.localiza2.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SuggestionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSuggestionsBinding? = null
    private val binding get() = _binding!!
    private val api by lazy { SuggestionsApiService.create() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSuggestionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-rellenar email del usuario logado
        val email = SessionManager(requireContext()).getEmail() ?: ""
        binding.etEmail.setText(email)

        binding.btnEnviar.setOnClickListener { submit() }
    }

    private fun submit() {
        val texto = binding.etTexto.text?.toString()?.trim() ?: ""
        val email = binding.etEmail.text?.toString()?.trim() ?: ""

        if (texto.isEmpty()) {
            binding.tilTexto.error = "Escribe tu sugerencia"
            return
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Email inválido"
            return
        }
        binding.tilTexto.error = null
        binding.tilEmail.error = null

        val tipo = when (binding.chipGroupTipo.checkedChipId) {
            binding.chipNuevaFunc.id -> 1
            binding.chipError.id    -> 2
            else                    -> 0
        }

        binding.btnEnviar.isEnabled = false
        binding.btnEnviar.text = "Enviando…"

        CoroutineScope(Dispatchers.IO).launch {
            val result = runCatching {
                api.sendSuggestion(SuggestionDto(email = email, texto = texto, tipo = tipo))
            }
            withContext(Dispatchers.Main) {
                if (result.isSuccess && (result.getOrNull()?.isSuccessful == true ||
                            result.getOrNull()?.code() == 201)) {
                    hideKeyboard()
                    Toast.makeText(requireContext(), "¡Gracias por tu sugerencia!", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    binding.btnEnviar.isEnabled = true
                    binding.btnEnviar.text = "Enviar"
                    Toast.makeText(requireContext(), "Error al enviar. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SuggestionsBottomSheet()
    }
}
