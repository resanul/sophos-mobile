package com.sophos.security.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.sophos.security.mobile.core.designsystem.theme.SophosSecurityTheme
import com.sophos.security.mobile.core.navigation.SophosNavGraph
import com.sophos.security.mobile.core.network.MockSecurityRepository

class MainActivity : FragmentActivity() {
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
