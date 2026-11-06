package com.verticalcut.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(onVideosPicked: (List<Uri>) -> Unit) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris -> if (uris.isNotEmpty()) onVideosPicked(uris) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Marca: un marco vertical con una línea de corte
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 112.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Accent),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(width = 48.dp, height = 96.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Bg)
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Accent2)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("VerticalCut", color = TextMain, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Edita en vertical, publica en segundos", color = TextDim, fontSize = 14.sp)
        Spacer(Modifier.height(40.dp))

        Button(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Nuevo proyecto", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        Text("Elige hasta 10 videos de tu galería", color = TextDim, fontSize = 13.sp)
    }
}
