package com.example.investigacion02

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.investigacion02.adapters.TaskAdapter
import com.example.investigacion02.models.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TaskListActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var lvTasks: ListView
    private lateinit var btnAddTask: Button
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()
    private var tasksListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        initViews()
        setupListeners()
        loadTasks()
    }

    private fun initViews() {
        lvTasks = findViewById(R.id.lvTasks)
        btnAddTask = findViewById(R.id.btnAddTask)

        taskAdapter = TaskAdapter(this, taskList, ::onTaskChecked, ::onTaskDelete, ::onTaskShare)
        lvTasks.adapter = taskAdapter
    }

    private fun setupListeners() {
        btnAddTask.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }
    }

    private fun loadTasks() {
        val currentUser = auth.currentUser ?: return

        // Escuchar cambios en las tareas propias
        val myTasksRef = database.child("tasks").child(currentUser.uid)

        tasksListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                taskList.clear()

                for (taskSnapshot in snapshot.children) {
                    val task = taskSnapshot.getValue(Task::class.java)
                    task?.let { taskList.add(it) }
                }

                // Cargar tareas compartidas
                loadSharedTasks()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@TaskListActivity,
                    "Error al cargar tareas: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        myTasksRef.addValueEventListener(tasksListener!!)
    }

    private fun loadSharedTasks() {
        val currentUser = auth.currentUser ?: return

        database.child("tasks").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    for (taskSnapshot in userSnapshot.children) {
                        val task = taskSnapshot.getValue(Task::class.java)
                        if (task != null &&
                            task.sharedWith.contains(currentUser.email) &&
                            !taskList.any { it.id == task.id }) {
                            taskList.add(task)
                        }
                    }
                }
                taskAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar error
            }
        })
    }

    private fun onTaskChecked(task: Task, isChecked: Boolean) {
        val currentUser = auth.currentUser ?: return

        // Solo el propietario puede modificar
        if (task.ownerId != currentUser.uid) {
            Toast.makeText(this, "Solo el propietario puede modificar esta tarea", Toast.LENGTH_SHORT).show()
            taskAdapter.notifyDataSetChanged()
            return
        }

        task.isCompleted = isChecked
        task.status = if (isChecked) Task.STATUS_FINISHED else Task.STATUS_IN_PROGRESS

        database.child("tasks").child(task.ownerId).child(task.id)
            .setValue(task)
            .addOnSuccessListener {
                Toast.makeText(this, "Tarea actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar tarea", Toast.LENGTH_SHORT).show()
            }
    }

    private fun onTaskDelete(task: Task) {
        val currentUser = auth.currentUser ?: return

        // Solo el propietario puede cancelar
        if (task.ownerId != currentUser.uid) {
            Toast.makeText(this, "Solo el propietario puede cancelar esta tarea", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Cancelar Tarea")
            .setMessage("¿Está seguro que desea cancelar esta tarea?")
            .setPositiveButton("Sí") { _, _ ->
                task.status = Task.STATUS_CANCELLED
                database.child("tasks").child(task.ownerId).child(task.id)
                    .setValue(task)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Tarea cancelada", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al cancelar tarea", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun onTaskShare(task: Task) {
        val currentUser = auth.currentUser ?: return

        // Solo el propietario puede compartir
        if (task.ownerId != currentUser.uid) {
            Toast.makeText(this, "Solo el propietario puede compartir esta tarea", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(this)
        input.hint = "correo@ejemplo.com"

        AlertDialog.Builder(this)
            .setTitle("Compartir Tarea")
            .setMessage("Ingrese el correo del usuario con quien desea compartir:")
            .setView(input)
            .setPositiveButton("Compartir") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    shareTask(task, email)
                } else {
                    Toast.makeText(this, "Ingrese un correo válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun shareTask(task: Task, email: String) {
        if (!task.sharedWith.contains(email)) {
            task.sharedWith.add(email)
            database.child("tasks").child(task.ownerId).child(task.id)
                .setValue(task)
                .addOnSuccessListener {
                    Toast.makeText(this, "Tarea compartida con $email", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al compartir tarea", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "La tarea ya está compartida con este usuario", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_task_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tasksListener?.let {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                database.child("tasks").child(currentUser.uid).removeEventListener(it)
            }
        }
    }
}