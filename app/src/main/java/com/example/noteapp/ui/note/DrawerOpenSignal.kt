package com.example.noteapp.ui.note

object DrawerOpenSignal {
    private var pending = false

    fun request() { pending = true }

    fun consume(): Boolean {
        val v = pending
        pending = false
        return v
    }
}
