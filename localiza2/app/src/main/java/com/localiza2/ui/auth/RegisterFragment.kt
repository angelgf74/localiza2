package com.localiza2.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.localiza2.api.RetrofitClient
import com.localiza2.databinding.FragmentRegisterBinding
import com.localiza2.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AuthViewModel
    private var pendingEmail: String? = null
    private var resendCooldownJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = AuthViewModel(RetrofitClient.create(SessionManager(requireContext())))

        binding.btnRegister.setOnClickListener { doRegister() }
        binding.btnGoLogin.setOnClickListener { parentFragmentManager.popBackStack() }
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
                            binding.btnRegister.isEnabled = false
                        }
                    }
                    is AuthState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRegister.isEnabled = true
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
                        binding.btnRegister.isEnabled = true
                        if (inPending) binding.btnResend.isEnabled = true
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun doRegister() {
        val name = binding.etName.text.toString()
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            Snackbar.make(binding.root, "Rellena todos los campos", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (password != confirm) {
            Snackbar.make(binding.root, "Las contraseñas no coinciden", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Snackbar.make(binding.root, "La contraseña debe tener al menos 6 caracteres", Snackbar.LENGTH_SHORT).show()
            return
        }
        viewModel.register(email, password, name)
    }

    private fun doResend() {
        val email = pendingEmail ?: return
        viewModel.resendConfirmation(email)
    }

    private fun showPendingState(email: String) {
        binding.layoutForm.visibility = View.GONE
        binding.layoutPending.visibility = View.VISIBLE
        binding.tvPendingDesc.text =
            "Hemos enviado un enlace de confirmación a $email. Revisa tu bandeja de entrada y pulsa el enlace para activar tu cuenta."
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
