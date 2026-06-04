package com.localiza2.api

import com.localiza2.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body dto: RegisterDto): Response<MessageResponse>

    @GET("api/auth/confirm/{token}")
    suspend fun confirmEmail(@Path("token") token: String): Response<MessageResponse>

    @POST("api/auth/login")
    suspend fun login(@Body dto: LoginDto): Response<LoginResponse>

    @GET("api/contacts")
    suspend fun getContacts(): Response<List<ContactDto>>

    @POST("api/contacts/invite")
    suspend fun inviteContact(@Body dto: InviteContactDto): Response<MessageResponse>

    @POST("api/contacts/pair/qr")
    suspend fun generatePairingQr(): com.localiza2.models.PairingQrResponse

    @PUT("api/contacts/{id}")
    suspend fun updateContact(@Path("id") id: Int, @Body dto: UpdateContactDto): Response<Unit>

    @DELETE("api/contacts/{id}")
    suspend fun deleteContact(@Path("id") id: Int): Response<Unit>

    @POST("api/location")
    suspend fun updateLocation(@Body dto: UpdateLocationDto): Response<Unit>

    @GET("api/location/contacts")
    suspend fun getContactsLocations(): Response<List<ContactLocationDto>>

    @GET("api/location/contacts/{contactId}")
    suspend fun getContactLocation(@Path("contactId") contactId: Int): Response<ContactLocationDto>

    @GET("api/location/contacts/{contactId}/history")
    suspend fun getContactLocationHistory(
        @Path("contactId") contactId: Int,
        @Query("limit") limit: Int = 50
    ): Response<List<LocationHistoryPointDto>>

    @POST("api/auth/resend-confirmation")
    suspend fun resendConfirmation(@Body dto: ForgotPasswordDto): Response<MessageResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body dto: ForgotPasswordDto): Response<MessageResponse>

    @DELETE("api/auth/delete-account")
    suspend fun deleteAccount(): Response<Unit>
}
