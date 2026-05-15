package com.skillexchange

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.skillexchange.ui.SkillExchangeRoot
import com.skillexchange.ui.theme.SkillExchangeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SkillExchangeTheme {
                SkillExchangeRoot()
            }
        }
    }
}
