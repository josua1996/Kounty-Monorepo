package com.kountyapp.network.model

data class TokenUpdateRequest(
    val usuarioId: String,  // Ahora contiene usuarioId
    val newToken: String
)
