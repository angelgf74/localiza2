package com.localiza2.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.localiza2.api.RetrofitClient
import com.localiza2.databinding.FragmentForgotPasswordBinding
import com.localiza2.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AuthViewModel
    private var pendingEmail: String? = null
    private var resendCooldownJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = AuthViewModel(RetrofitClient.create(SessionManager(requireContext())))

        binding.btnSend.setOnClickListener { doSend() }
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnResend.setOnClickListener { doResend() }
        binding.btnBackToLogin.setOnClickListener { parentFragmentManager.popBackStack() }

        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                val inPending = pendingEmail != null
                when (state) {
                    is AuthState.Loading -> {
                        if (inPending) {
                            binding.btnResend.isEnabled = false
                        } else {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnSend.isEnabled = false
                        }
                    }
                    is AuthState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSend.isEnabled = true
                        if (!inPending) {
                            pendingEmail = binding.etEmail.text.toString().trim()
                            showPendingState(pendingEmail!!)
                        } else {
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        }
                        startResendCooldown()
                        viewModel.resetState()
                    }
                    is AuthState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSend.isEnabled = true
                        if (inPending) binding.btnResend.isEnabled = true
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun doSend() {
        val email = binding.etEmail.text.toString().trim()
        if (email.isBlank()) {
            Snackbar.make(binding.root, "Introduce tu correo", Snackbar.LENGTH_SHORT).show()
            return
        }
        viewModel.forgotPassword(email)
    }

    private fun doResend() {
        val email = pendingEmail ?: return
        viewModel.forgotPassword(email)
    }

    private fun showPendingState(email: String) {
        binding.layoutForm.visibility = View.GONE
        binding.layoutPending.visibility = View.VISIBLE
        binding.tvPendingDesc.text =
            "Si $email está registrado, recibirás un enlace para restablecer tu contraseña. Revisa tu bandeja de entrada."
    }

    private fun startResendCooldown() {
        resendCooldownJob?.cancel()
        binding.btnResend.isEnabled = false
        resendCooldownJob = lifecycleScope.launch {
            for (secs in 60 downTo 1) {
                binding.btnResend.text = "Reenviar correo ($secs s)"
                delay(1000)
            }
            binding.btnResend.text = "Reenviar correo"
            binding.btnResend.isEnabled = true
        }
    }

    override fun onDestroyView() {
        resendCooldownJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
