package com.skillexchange

import android.app.Application
import com.google.firebase.FirebaseApp

class SkillExchangeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
