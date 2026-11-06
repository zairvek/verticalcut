package com.verticalcut.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verticalcut.app.edit.EditorViewModel
import com.verticalcut.app.ui.EditorScreen
import com.verticalcut.app.ui.HomeScreen
import com.verticalcut.app.ui.VerticalCutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VerticalCutTheme {
                val vm: EditorViewModel = viewModel()
                var inEditor by remember { mutableStateOf(vm.hasClips()) }

                if (inEditor) {
                    BackHandler { inEditor = false }
                    EditorScreen(vm = vm, onBack = { inEditor = false })
                } else {
                    HomeScreen(onVideosPicked = { uris ->
                        vm.addClips(uris)
                        inEditor = true
                    })
                }
            }
        }
    }
}
