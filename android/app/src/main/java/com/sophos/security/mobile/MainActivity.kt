package com.sophos.security.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sophos.security.mobile.core.designsystem.theme.SophosSecurityTheme
import com.sophos.security.mobile.core.navigation.SophosNavGraph
import com.sophos.security.mobile.core.network.MockSecurityRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MockSecurityRepository.initStorage(applicationContext)
        enableEdgeToEdge()
        setContent {
            SophosSecurityTheme(darkTheme = true) {
                SophosNavGraph()
            }
        }
    }
}
