package com.astrid0049.myskin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.astrid0049.myskin.database.SkincareDatabase
import com.astrid0049.myskin.network.UserDataStore
import com.astrid0049.myskin.ui.screen.MainScreen
import com.astrid0049.myskin.ui.screen.MainViewModel
import com.astrid0049.myskin.ui.theme.MySkinTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = SkincareDatabase.getDatabase(this)
        val userDataStore = UserDataStore(this)

        setContent {
            MySkinTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LaunchedEffect(Unit) {
                        viewModel.initAuth(userDataStore, database.skincareDao())
                    }

                    MainScreen(
                        viewModel = viewModel,
                        dao = database.skincareDao(),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}