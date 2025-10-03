package com.example.investigacion02

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.investigacion02.models.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class AddTaskActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var etTaskName: EditText
    private lateinit var etAssignedTo: EditText
    private lateinit var btnStartDateTime: Button
    private lateinit var btnEndDateTime: Button
    private lateinit var spinnerStatus: Spinner
    private lateinit var btnSaveTask: Button
    private lateinit var tvStartDateTime: TextView
    private lateinit var tvEndDateTime: TextView

    private var startDateTime: Long = 0L
    private var endDateTime: Long = 0L
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Nueva Tarea"

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        initViews()
        setupStatusSpinner()
        setupListeners()
    }

    private fun initViews() {
        etTaskName = findViewById(R.id.etTaskName)
        etAssignedTo = findViewById(R.id.etAssignedTo)
        btnStartDateTime = findViewById(R.id.btnStartDateTime)
        btnEndDateTime = findViewById(R.id.btnEndDateTime)
        spinnerStatus = findViewById(R.id.spinnerStatus)
        btnSaveTask = findViewById(R.id.btnSaveTask)
        tvStartDateTime = findViewById(R.id.tvStartDateTime)
        tvEndDateTime = findViewById(R.id.tvEndDateTime)
    }

    private fun setupStatusSpinner() {
        val statuses = arrayOf(
            Task.STATUS_SCHEDULED,
            Task.STATUS_IN_PROGRESS,
            Task.STATUS_FINISHED,
            Task.STATUS_CANCELLED
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statuses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = adapter
    }

    private fun setupListeners() {
        btnStartDateTime.setOnClickListener {
            showDateTimePicker(true)
        }

        btnEndDateTime.setOnClickListener {
            showDateTimePicker(false)
        }

        btnSaveTask.setOnClickListener {
            saveTask()
        }
    }

    private fun showDateTimePicker(isStartDateTime: Boolean) {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)

                        val timeInMillis = calendar.timeInMillis

                        if (isStartDateTime) {
                            startDateTime = timeInMillis
                            tvStartDateTime.text = "Inicio: ${dateFormat.format(Date(timeInMillis))}"
                        } else {
                            endDateTime = timeInMillis
                            tvEndDateTime.text = "Fin: ${dateFormat.format(Date(timeInMillis))}"
                        }
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun saveTask() {
        val taskName = etTaskName.text.toString().trim()
        val assignedTo = etAssignedTo.text.toString().trim()
        val status = spinnerStatus.selectedItem.toString()

        if (taskName.isEmpty()) {
            etTaskName.error = "Ingrese el nombre de la tarea"
            etTaskName.requestFocus()
            return
        }

        if (assignedTo.isEmpty()) {
            etAssignedTo.error = "Ingrese el encargado"
            etAssignedTo.requestFocus()
            return
        }

        if (startDateTime == 0L) {
            Toast.makeText(this, "Seleccione la fecha y hora de inicio", Toast.LENGTH_SHORT).show()
            return
        }

        if (endDateTime == 0L) {
            Toast.makeText(this, "Seleccione la fecha y hora de fin", Toast.LENGTH_SHORT).show()
            return
        }

        if (endDateTime <= startDateTime) {
            Toast.makeText(this, "La fecha de fin debe ser posterior a la de inicio", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser ?: return
        val taskId = database.child("tasks").child(currentUser.uid).push().key ?: return

        val task = Task(
            id = taskId,
            name = taskName,
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            status = status,
            assignedTo = assignedTo,
            ownerId = currentUser.uid,
            ownerEmail = currentUser.email ?: "",
            sharedWith = mutableListOf(),
            isCompleted = status == Task.STATUS_FINISHED
        )

        database.child("tasks").child(currentUser.uid).child(taskId)
            .setValue(task)
            .addOnSuccessListener {
                Toast.makeText(this, "Tarea guardada exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar tarea: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}