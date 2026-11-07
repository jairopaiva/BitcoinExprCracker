package com.jairopaiva.exprlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jairopaiva.exprlab.ui.AppRoot
import com.jairopaiva.exprlab.ui.theme.ExprLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExprLabTheme {
                AppRoot()
            }
        }
    }
}
