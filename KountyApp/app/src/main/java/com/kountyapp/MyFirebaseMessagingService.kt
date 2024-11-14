package com.kountyapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kountyapp.network.RetrofitClient
import com.kountyapp.network.model.TokenUpdateRequest  // Asegúrate de que esté importado TokenUpdateRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Procesar los datos del mensaje
        if (remoteMessage.data.isNotEmpty()) {
            val clavePersonalizada = remoteMessage.data["clavePersonalizada"]
            val tipoImpuesto = remoteMessage.data["tipoImpuesto"]
            val fecha = remoteMessage.data["fecha"]

            Log.d("FCM", "Clave personalizada: $clavePersonalizada")
            Log.d("FCM", "Tipo de impuesto: $tipoImpuesto")
            Log.d("FCM", "Fecha: $fecha")
        }

        // Si el mensaje incluye una notificación, mostrarla en el dispositivo
        remoteMessage.notification?.let {
            showNotification(it.body ?: "Nueva notificación de Kounty")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM Token", "Nuevo token: $token")
        sendTokenToServer(token)
    }

    private fun showNotification(messageBody: String) {
        val channelId = "default_channel"
        createNotificationChannel(channelId)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Notificación de Kounty")
            .setContentText(messageBody)
            .setSmallIcon(R.drawable.ic_kounty_icon)  // Asegúrate de que exista este ícono
            .setAutoCancel(true)
            .build()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notification)
    }

    private fun createNotificationChannel(channelId: String) {
        val channelName = "Canal de Notificaciones de Kounty"
        val channelDescription = "Canal predeterminado para notificaciones de la aplicación Kounty"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = channelDescription
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun sendTokenToServer(token: String) {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            // Obtener el usuarioId (el UID del usuario autenticado)
            val usuarioId = user.uid

            // Se pasa el usuarioId junto con el token en el TokenUpdateRequest
            val tokenUpdateRequest = TokenUpdateRequest(newToken = token, usuarioId = usuarioId)

            // Refresca el token de autenticación antes de enviarlo al servidor
            user.getIdToken(true).addOnSuccessListener { result ->
                val authToken = result.token ?: ""

                // Asegúrate de que el token de autenticación se pase como parte de los encabezados
                RetrofitClient.apiService.actualizarToken(tokenUpdateRequest, authToken)
                    .enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            if (!response.isSuccessful) {
                                Log.e("FCM Token", "Error al enviar token al servidor")
                            }
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Log.e("FCM Token", "Error de red al enviar token: ${t.message}")
                        }
                    })
            }
        } ?: run {
            Log.e("FCM Token", "Usuario no autenticado, no se puede enviar token al servidor")
        }
    }
}
