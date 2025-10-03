package com.example.investigacion02.adapters

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.example.investigacion02.R
import com.example.investigacion02.models.Task
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val context: Context,
    private val tasks: List<Task>,
    private val onTaskChecked: (Task, Boolean) -> Unit,
    private val onTaskDelete: (Task) -> Unit,
    private val onTaskShare: (Task) -> Unit
) : BaseAdapter() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val auth = FirebaseAuth.getInstance()

    override fun getCount(): Int = tasks.size

    override fun getItem(position: Int): Any = tasks[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_task, parent, false)

        val task = tasks[position]
        val currentUser = auth.currentUser
        val isOwner = task.ownerId == currentUser?.uid

        val tvTaskName = view.findViewById<TextView>(R.id.tvTaskName)
        val tvTaskDetails = view.findViewById<TextView>(R.id.tvTaskDetails)
        val tvTaskStatus = view.findViewById<TextView>(R.id.tvTaskStatus)
        val tvTaskOwner = view.findViewById<TextView>(R.id.tvTaskOwner)
        val cbTaskCompleted = view.findViewById<CheckBox>(R.id.cbTaskCompleted)
        val btnCancelTask = view.findViewById<Button>(R.id.btnCancelTask)
        val btnShareTask = view.findViewById<Button>(R.id.btnShareTask)

        // Configurar información de la tarea
        tvTaskName.text = task.name
        tvTaskDetails.text = "Encargado: ${task.assignedTo}\n" +
                "Inicio: ${dateFormat.format(Date(task.startDateTime))}\n" +
                "Fin: ${dateFormat.format(Date(task.endDateTime))}"
        tvTaskStatus.text = "Estado: ${task.status}"

        // Mostrar si es tarea compartida
        if (!isOwner) {
            tvTaskOwner.visibility = View.VISIBLE
            tvTaskOwner.text = "Compartida por: ${task.ownerEmail}"
        } else {
            tvTaskOwner.visibility = View.GONE
        }

        // Configurar checkbox
        cbTaskCompleted.isChecked = task.isCompleted
        cbTaskCompleted.setOnCheckedChangeListener { _, isChecked ->
            onTaskChecked(task, isChecked)
        }

        // Deshabilitar checkbox si no es propietario
        cbTaskCompleted.isEnabled = isOwner &&
                task.status != Task.STATUS_CANCELLED

        // Configurar botón de cancelar
        btnCancelTask.isEnabled = isOwner &&
                task.status != Task.STATUS_CANCELLED &&
                task.status != Task.STATUS_FINISHED

        btnCancelTask.setOnClickListener {
            onTaskDelete(task)
        }

        // Configurar botón de compartir
        btnShareTask.visibility = if (isOwner) View.VISIBLE else View.GONE
        btnShareTask.setOnClickListener {
            onTaskShare(task)
        }

        // Aplicar estilo según estado
        when (task.status) {
            Task.STATUS_FINISHED -> {
                tvTaskName.paintFlags = tvTaskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                view.alpha = 0.6f
                view.setBackgroundColor(context.getColor(android.R.color.holo_green_light))
            }
            Task.STATUS_CANCELLED -> {
                tvTaskName.paintFlags = tvTaskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                view.alpha = 0.5f
                view.setBackgroundColor(context.getColor(android.R.color.holo_red_light))
            }
            Task.STATUS_IN_PROGRESS -> {
                tvTaskName.paintFlags = tvTaskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                view.alpha = 1.0f
                view.setBackgroundColor(context.getColor(android.R.color.holo_orange_light))
            }
            else -> {
                tvTaskName.paintFlags = tvTaskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                view.alpha = 1.0f
                view.setBackgroundColor(context.getColor(android.R.color.white))
            }
        }

        return view
    }
}