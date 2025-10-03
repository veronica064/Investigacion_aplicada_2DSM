package com.example.investigacion02

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Verificar si el usuario ya está autenticado
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Usuario ya autenticado, ir a la lista de tareas
            startActivity(Intent(this, TaskListActivity::class.java))
        } else {
            // No hay usuario autenticado, ir al login
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}