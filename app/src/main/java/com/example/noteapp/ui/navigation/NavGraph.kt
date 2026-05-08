package com.example.noteapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.noteapp.ui.note.DateNotesScreen
import com.example.noteapp.ui.note.MainScreen
import com.example.noteapp.ui.note.NoteEditorScreen
import com.example.noteapp.ui.note.NoteViewModel
import com.example.noteapp.ui.note.RecycleBinScreen
import com.example.noteapp.ui.sidebar.SettingsScreen
import com.example.noteapp.util.Logger

object Routes {
    const val MAIN = "main"
    const val NOTE_EDITOR = "note/{noteId}"
    const val DATE_NOTES = "date-notes/{dateStr}"
    const val SETTINGS = "settings"
    const val RECYCLE_BIN = "recycle-bin"

    fun noteEditor(noteId: Long) = "note/$noteId"
    fun dateNotes(dateStr: String) = "date-notes/$dateStr"
}

@Composable
fun NoteNavGraph(navController: NavHostController) {
    val noteViewModel: NoteViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                viewModel = noteViewModel,
                onNoteClick = { noteId ->
                    Logger.i("Nav", "打开笔记编辑 id=$noteId")
                    navController.navigate(Routes.noteEditor(noteId))
                },
                onDateClick = { dateStr ->
                    Logger.i("Nav", "打开日期笔记 $dateStr")
                    navController.navigate(Routes.dateNotes(dateStr))
                },
                onSettings = {
                    Logger.i("Nav", "打开设置")
                    navController.navigate(Routes.SETTINGS)
                },
                onRecycleBin = {
                    Logger.i("Nav", "打开回收站")
                    navController.navigate(Routes.RECYCLE_BIN)
                }
            )
        }

        composable(
            route = Routes.DATE_NOTES,
            arguments = listOf(
                navArgument("dateStr") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val dateStr = backStackEntry.arguments?.getString("dateStr") ?: ""
            DateNotesScreen(
                dateStr = dateStr,
                onBack = { navController.popBackStack() },
                onNoteClick = { noteId -> navController.navigate(Routes.noteEditor(noteId)) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = {
                com.example.noteapp.ui.note.DrawerOpenSignal.request()
                navController.popBackStack()
            })
        }

        composable(Routes.RECYCLE_BIN) {
            RecycleBinScreen(
                viewModel = noteViewModel,
                onBack = {
                    com.example.noteapp.ui.note.DrawerOpenSignal.request()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.NOTE_EDITOR,
            arguments = listOf(
                navArgument("noteId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            NoteEditorScreen(
                noteId = noteId,
                viewModel = noteViewModel,
                onBack = { navController.popBackStack().let { if (!it) navController.navigate(Routes.MAIN) } }
            )
        }
    }
}
