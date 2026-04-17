package com.example.mobapp

data class Habit(
    val id: Int,
    val name: String,
    val intervalMinutes: Int,
    val enabled: Boolean,
    val lastDoneAt: Long
)
