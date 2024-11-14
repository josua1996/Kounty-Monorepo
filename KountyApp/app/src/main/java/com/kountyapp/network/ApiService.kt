package com.kountyapp.network

import com.kountyapp.network.model.Recordatorio
import com.kountyapp.network.model.TokenUpdateRequest
import com.kountyapp.network.model.UsuarioRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("/agregar-recordatorio")
    fun agregarRecordatorio(
        @Body recordatorio: Recordatorio
    ): Call<Void>

    @GET("/recordatorios")
    fun obtenerRecordatorios(): Call<List<Recordatorio>>

    @POST("/register-usuario")
    fun crearUsuario(
        @Body usuarioRequest: UsuarioRequest
    ): Call<Void>

    @POST("/actualizar-token")
    fun actualizarToken(
        @Body tokenUpdateRequest: TokenUpdateRequest,
        @Header("Authorization") authToken: String
    ): Call<Void>
}
