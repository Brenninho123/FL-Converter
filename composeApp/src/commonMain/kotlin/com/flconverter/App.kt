package com.flconverter

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flconverter.ui.ConverterScreen
import com.flconverter.ui.FlConverterTheme
import com.flconverter.ui.MusicTrail
import com.flconverter.ui.musicCursor

@Composable
fun App() {
    FlConverterTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            MusicTrail(Modifier.fillMaxSize().musicCursor()) {
                ConverterScreen()
            }
        }
    }
}
