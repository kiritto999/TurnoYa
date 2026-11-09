package com.yperez.turnoya.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Endpoints de la API TurnoYa (FastAPI). El token se agrega automáticamente con un interceptor. */
interface TurnoYaApi {
    // Auth
    @POST("api/v1/auth/login") suspend fun login(@Body body: LoginRequest): TokenResponse
    @POST("api/v1/auth/register") suspend fun register(@Body body: RegisterRequest): User
    @GET("api/v1/auth/me") suspend fun me(): User

    // Negocios
    @GET("api/v1/businesses")
    suspend fun businesses(
        @Query("nombre") nombre: String? = null,
        @Query("categoria") categoria: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radio_km") radioKm: Double? = null,
    ): List<Business>

    @GET("api/v1/businesses/categories") suspend fun categories(): List<String>
    @GET("api/v1/businesses/mine") suspend fun myBusinesses(): List<Business>
    @GET("api/v1/businesses/{id}") suspend fun business(@Path("id") id: Int): Business
    @GET("api/v1/businesses/{id}/stats") suspend fun stats(@Path("id") id: Int): BusinessStats
    @POST("api/v1/businesses") suspend fun createBusiness(@Body body: BusinessRequest): Business
    @PUT("api/v1/businesses/{id}") suspend fun updateBusiness(@Path("id") id: Int, @Body body: BusinessRequest): Business

    // Sectores
    @GET("api/v1/businesses/{id}/sectors") suspend fun sectors(@Path("id") businessId: Int): List<Sector>
    @POST("api/v1/businesses/{id}/sectors") suspend fun createSector(@Path("id") businessId: Int, @Body body: SectorRequest): Sector
    @PUT("api/v1/sectors/{id}") suspend fun updateSector(@Path("id") id: Int, @Body body: SectorRequest): Sector
    @DELETE("api/v1/sectors/{id}") suspend fun deleteSector(@Path("id") id: Int): Response<Unit>

    // Catálogo
    @GET("api/v1/businesses/{id}/catalog") suspend fun catalog(@Path("id") businessId: Int): List<CatalogItem>
    @POST("api/v1/businesses/{id}/catalog") suspend fun createItem(@Path("id") businessId: Int, @Body body: CatalogItemRequest): CatalogItem
    @PUT("api/v1/catalog/{id}") suspend fun updateItem(@Path("id") id: Int, @Body body: CatalogItemRequest): CatalogItem
    @DELETE("api/v1/catalog/{id}") suspend fun deleteItem(@Path("id") id: Int): Response<Unit>

    // Turnos
    @POST("api/v1/businesses/{id}/turns") suspend fun requestTurn(@Path("id") businessId: Int, @Body body: TurnRequest): Turn
    @GET("api/v1/turns/my") suspend fun myTurns(): List<Turn>
    @GET("api/v1/turns/{id}") suspend fun turn(@Path("id") id: Int): Turn
    @GET("api/v1/businesses/{id}/queue") suspend fun queue(@Path("id") businessId: Int, @Query("sector_id") sectorId: Int? = null): List<Turn>
    @POST("api/v1/businesses/{id}/queue/next") suspend fun callNext(@Path("id") businessId: Int, @Query("sector_id") sectorId: Int? = null): Turn
    @PATCH("api/v1/turns/{id}/status") suspend fun setTurnStatus(@Path("id") id: Int, @Body body: TurnStatusRequest): Turn

    // QR
    @GET("api/v1/businesses/{id}/qr") suspend fun qrCodes(@Path("id") businessId: Int): List<QrCode>
    @POST("api/v1/businesses/{id}/qr") suspend fun createQr(@Path("id") businessId: Int, @Body body: QrRequest): QrCode
    @GET("api/v1/qr/{code}") suspend fun resolveQr(@Path("code") code: String): QrCode

    // Empleados
    @GET("api/v1/businesses/{id}/employees") suspend fun employees(@Path("id") businessId: Int): List<Employee>
    @POST("api/v1/businesses/{id}/employees/invitations") suspend fun invite(@Path("id") businessId: Int, @Body body: InviteRequest): Map<String, Any>
    @DELETE("api/v1/employees/{id}") suspend fun removeEmployee(@Path("id") id: Int): Response<Unit>
    @GET("api/v1/employee-invitations/mine") suspend fun myInvitations(): List<Employee>
    @POST("api/v1/employee-invitations/by-id/{id}/accept") suspend fun acceptInvitation(@Path("id") id: Int): Employee
    @POST("api/v1/employee-invitations/by-id/{id}/reject") suspend fun rejectInvitation(@Path("id") id: Int): Employee

    // Notificaciones
    @GET("api/v1/notifications") suspend fun notifications(): List<AppNotification>
    @GET("api/v1/notifications/unread-count") suspend fun unreadCount(): UnreadCount
    @POST("api/v1/notifications/read-all") suspend fun readAll(): Response<Unit>
    @PATCH("api/v1/notifications/{id}/read") suspend fun markRead(@Path("id") id: Int): AppNotification
}
