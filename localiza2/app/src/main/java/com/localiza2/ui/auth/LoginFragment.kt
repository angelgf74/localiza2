package com.localiza2.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.localiza2.BuildConfig
import com.localiza2.api.RetrofitClient
import com.localiza2.databinding.FragmentLoginBinding
import com.localiza2.ui.MainActivity
import com.localiza2.utils.SessionManager
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AuthViewModel
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        viewModel = AuthViewModel(RetrofitClient.create(sessionManager))

        binding.tvVersion.text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

        binding.btnLogin.setOnClickListener { doLogin() }
        binding.btnForgotPassword.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace((requireView().parent as View).id, ForgotPasswordFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.btnGoRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace((requireView().parent as View).id, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                binding.progressBar.visibility = if (state is AuthState.Loading) View.VISIBLE else View.GONE
                binding.btnLogin.isEnabled = state !is AuthState.Loading
                if (state is AuthState.Error) Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
        }

        lifecycleScope.launch {
            viewModel.loginResult.collect { result ->
                result ?: return@collect
                sessionManager.saveSession(result.token, result.userId, result.name, result.email)
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            }
        }
    }

    private fun doLogin() {
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        if (email.isBlank() || password.isBlank()) {
            Snackbar.make(binding.root, "Rellena todos los campos", Snackbar.LENGTH_SHORT).show()
            return
        }
        viewModel.login(email, password)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
