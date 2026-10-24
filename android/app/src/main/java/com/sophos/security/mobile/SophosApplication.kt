package com.sophos.security.mobile

import android.app.Application
import com.sophos.security.mobile.core.network.MockSecurityRepository
import com.sophos.security.mobile.core.security.EncryptedStorageManager

class SophosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        EncryptedStorageManager.init(this)
        MockSecurityRepository.initStorage(this)
    }
}
