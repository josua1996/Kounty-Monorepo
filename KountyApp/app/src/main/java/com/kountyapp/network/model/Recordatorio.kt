package com.kountyapp.network.model

import java.util.*

data class Recordatorio(
    val usuario: String,
    val fecha: Date,  // Sigue siendo un tipo Date
    val tipoImpuesto: String,
    val mensaje: String
)
