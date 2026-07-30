package com.example.wikishorts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.wikishorts.ui.WikiShortsApp
import com.example.wikishorts.ui.theme.WikiShortsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WikiShortsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WikiShortsApp()
                }
            }
        }
    }
}
