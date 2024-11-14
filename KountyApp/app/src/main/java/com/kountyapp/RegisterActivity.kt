package com.kountyapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.kountyapp.network.RetrofitClient
import com.kountyapp.network.model.UsuarioRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Obtener las vistas de los campos de entrada
        val emailEditText = findViewById<EditText>(R.id.email)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val registerButton = findViewById<Button>(R.id.register_button)

        // Establecer el comportamiento para el botón de registro
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            registerUser(email, password)
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Usuario registrado exitosamente
                    val user = auth.currentUser
                    user?.let {
                        // Refrescar el token antes de registrar al usuario en el servidor
                        refreshFcmTokenAndRegisterUser(user.uid)
                    }
                } else {
                    // Error al registrar el usuario
                    Log.e("com.kountyapp.RegisterActivity", "Registration failed", task.exception)
                }
            }
    }

    private fun refreshFcmTokenAndRegisterUser(usuarioId: String) {
        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { deleteTask ->
            if (deleteTask.isSuccessful) {
                // Refrescar el token FCM después de eliminar el anterior
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Obtener el nuevo token FCM
                        val token = task.result

                        // Crear el objeto UsuarioRequest con el usuarioId y el nuevo token
                        val usuarioRequest = UsuarioRequest(usuarioId, token)

                        // Enviar la solicitud al servidor
                        sendUserToServer(usuarioRequest)
                    } else {
                        Log.e("com.kountyapp.RegisterActivity", "FCM Token retrieval failed", task.exception)
                    }
                }
            } else {
                Log.e("com.kountyapp.RegisterActivity", "Failed to delete FCM Token", deleteTask.exception)
            }
        }
    }

    private fun sendUserToServer(usuarioRequest: UsuarioRequest) {
        val call = RetrofitClient.apiService.crearUsuario(usuarioRequest)  // Cambio aquí para ajustar a `crearUsuario`
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("com.kountyapp.RegisterActivity", "User registered successfully on server")
                } else {
                    Log.e("com.kountyapp.RegisterActivity", "Failed to register user on server: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("com.kountyapp.RegisterActivity", "Network failure: ${t.message}")
            }
        })
    }
}
