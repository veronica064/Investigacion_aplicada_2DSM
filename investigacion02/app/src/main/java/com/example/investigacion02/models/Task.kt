package com.example.investigacion02.models

data class Task(
    var id: String = "",
    var name: String = "",
    var startDateTime: Long = 0L,
    var endDateTime: Long = 0L,
    var status: String = "Programada",
    var assignedTo: String = "",
    var ownerId: String = "",
    var ownerEmail: String = "",
    var sharedWith: MutableList<String> = mutableListOf(),
    var isCompleted: Boolean = false
) {
    companion object {
        const val STATUS_SCHEDULED = "Programada"
        const val STATUS_IN_PROGRESS = "En Proceso"
        const val STATUS_FINISHED = "Finalizado"
        const val STATUS_CANCELLED = "Cancelado"
    }
}