package com.kountyapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.kountyapp.network.RetrofitClient
import com.kountyapp.network.model.Recordatorio
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReminderActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        val tipoEditText = findViewById<EditText>(R.id.tipoImpuesto)
        val mensajeEditText = findViewById<EditText>(R.id.mensaje)
        val fechaEditText = findViewById<EditText>(R.id.fecha)
        val saveButton = findViewById<Button>(R.id.agregar_recordatorio_button)

        saveButton.setOnClickListener {
            val tipoImpuesto = tipoEditText.text.toString()
            val mensaje = mensajeEditText.text.toString()
            val fechaString = fechaEditText.text.toString()

            if (tipoImpuesto.isNotEmpty() && mensaje.isNotEmpty() && fechaString.isNotEmpty()) {
                saveReminder(tipoImpuesto, mensaje, fechaString)
            } else {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveReminder(tipoImpuesto: String, mensaje: String, fechaString: String) {
        val userId = auth.currentUser?.uid ?: return

        // Convierte el String de fecha a Date
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())  // Asegúrate de que el formato sea el adecuado
        val fecha: Date = try {
            format.parse(fechaString) ?: Date()  // Si no se puede parsear, usa la fecha actual
        } catch (e: Exception) {
            Log.e("ReminderActivity", "Error al convertir la fecha: ${e.message}")
            Date()  // Usamos la fecha actual si ocurre un error
        }

        // Crear el objeto Recordatorio
        val recordatorio = Recordatorio(userId, fecha, tipoImpuesto, mensaje)

        // Llamada a la API para guardar el recordatorio
        RetrofitClient.apiService.agregarRecordatorio(recordatorio).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("Recordatorio", "Recordatorio guardado correctamente")
                    Toast.makeText(this@ReminderActivity, "Recordatorio guardado", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("Recordatorio", "Error al guardar el recordatorio")
                    Toast.makeText(this@ReminderActivity, "Error al guardar el recordatorio", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("Recordatorio", "Error de red: ${t.message}")
                Toast.makeText(this@ReminderActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
