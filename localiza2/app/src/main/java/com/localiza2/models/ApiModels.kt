package com.localiza2.models

data class RegisterDto(val email: String, val password: String, val name: String)
data class LoginDto(val email: String, val password: String)
data class ForgotPasswordDto(val email: String)
data class LoginResponse(val token: String, val userId: Int, val name: String, val email: String)
data class MessageResponse(val message: String)

data class InviteContactDto(val email: String, val alias: String)
data class UpdateContactDto(val alias: String, val photoUrl: String?)
data class ContactDto(
    val id: Int,
    val email: String,
    val alias: String,
    val photoUrl: String?,
    val status: String,
    val locationPermissionGranted: Boolean,
    val contactUserId: Int?
)

data class PairingQrResponse(val token: String, val expiresAt: String) {
    val expiresAtMillis: Long get() =
        java.time.Instant.parse(expiresAt).toEpochMilli()
}

data class UpdateLocationDto(val latitude: Double, val longitude: Double, val accuracy: Double?, val batteryLevel: Int? = null)

data class SuggestionDto(
    @com.google.gson.annotations.SerializedName("Email")    val email: String,
    @com.google.gson.annotations.SerializedName("Texto")    val texto: String,
    @com.google.gson.annotations.SerializedName("Aplicacion") val aplicacion: String = "localiza2",
    @com.google.gson.annotations.SerializedName("Tipo")    val tipo: Int = 0
)
data class ContactLocationDto(
    val contactId: Int,
    val alias: String,
    val photoUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double?,
    val timestamp: String,
    val batteryLevel: Int? = null
)

data class LocationHistoryPointDto(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double?,
    val timestamp: String
)
