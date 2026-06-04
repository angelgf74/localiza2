package com.localiza2.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localiza2.api.ApiService
import com.localiza2.models.ForgotPasswordDto
import com.localiza2.models.LoginDto
import com.localiza2.models.RegisterDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class LoginSuccess(val token: String, val userId: Int, val name: String, val email: String)

class AuthViewModel(private val api: ApiService) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _loginResult = MutableStateFlow<LoginSuccess?>(null)
    val loginResult: StateFlow<LoginSuccess?> = _loginResult

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            runCatching {
                api.register(RegisterDto(email.trim(), password, name.trim()))
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    _authState.value = AuthState.Success(response.body()?.message ?: "Revisa tu correo.")
                } else {
                    val errorMsg = try {
                        val json = response.errorBody()?.string()
                        org.json.JSONObject(json ?: "").optString("message", "Error ${response.code()}")
                    } catch (e: Exception) {
                        "Error ${response.code()}"
                    }
                    _authState.value = AuthState.Error(errorMsg)
                }
            }.onFailure {
                _authState.value = AuthState.Error("Error de conexión: ${it.message}")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            runCatching {
                api.login(LoginDto(email.trim(), password))
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _loginResult.value = LoginSuccess(body.token, body.userId, body.name, body.email)
                    _authState.value = AuthState.Success("Bienvenido")
                } else if (response.code() == 401) {
                    _authState.value = AuthState.Error("Credenciales incorrectas")
                } else {
                    _authState.value = AuthState.Error("Error del servidor, inténtalo de nuevo")
                }
            }.onFailure {
                val msg = when (it) {
                    is java.net.SocketTimeoutException -> "Sin respuesta del servidor, inténtalo de nuevo"
                    is java.io.IOException -> "Sin conexión, verifica tu red"
                    else -> "Error de conexión"
                }
                _authState.value = AuthState.Error(msg)
            }
        }
    }

    fun resendConfirmation(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            runCatching { api.resendConfirmation(ForgotPasswordDto(email.trim())) }
                .onSuccess { response ->
                    _authState.value = if (response.isSuccessful)
                        AuthState.Success(response.body()?.message ?: "Correo reenviado.")
                    else
                        AuthState.Error("No se pudo reenviar el correo.")
                }
                .onFailure { _authState.value = AuthState.Error("Error de conexión: ${it.message}") }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            runCatching { api.forgotPassword(ForgotPasswordDto(email.trim())) }
                .onSuccess { response ->
                    _authState.value = if (response.isSuccessful)
                        AuthState.Success(response.body()?.message ?: "Revisa tu correo.")
                    else {
                        val msg = try {
                            org.json.JSONObject(response.errorBody()?.string() ?: "")
                                .optString("message", "Error ${response.code()}")
                        } catch (e: Exception) { "Error ${response.code()}" }
                        AuthState.Error(msg)
                    }
                }
                .onFailure { _authState.value = AuthState.Error("Error de conexión: ${it.message}") }
        }
    }

    fun resetState() { _authState.value = AuthState.Idle }
}
