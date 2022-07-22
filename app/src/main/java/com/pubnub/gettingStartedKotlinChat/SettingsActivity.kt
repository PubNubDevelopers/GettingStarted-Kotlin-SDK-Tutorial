package com.pubnub.gettingStartedKotlinChat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import com.pubnub.gettingStartedKotlinChat.ui.theme.GettingStartedChatTheme
import com.pubnub.gettingStartedKotlinChat.ui.view.SettingsUI.SettingsHeader
import com.pubnub.gettingStartedKotlinChat.ui.view.SettingsUI.SettingsOptionFriendlyName
import com.pubnub.gettingStartedKotlinChat.ui.view.SettingsUI.SettingsWelcomeText

class SettingsActivity : ComponentActivity() {
    private val currentActivity = this

    //  The Settings Activity is used to define a friendly name for the current device.  This will
    //  replace the machine-assigned UUID that the app is given at startup.
    //  Friendly names will be persisted in PubNub object storage (when the user presses save)
    //  For simplicity, please see the ui.view.SettingsUI.kt for the button handler code
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launchIntent = intent
        val mFriendlyName = launchIntent.getStringExtra("current_friendly_name").toString()
        val mDeviceId = launchIntent.getStringExtra("device_id").toString()
        val mPublishKey = launchIntent.getStringExtra("publish_key").toString()
        val mSubscribeKey = launchIntent.getStringExtra("subscribe_key").toString()
        val mOriginalFriendlyName = mFriendlyName
        setContent {
            GettingStartedChatTheme {
                Column()
                {
                    SettingsHeader()
                    SettingsWelcomeText()
                    SettingsOptionFriendlyName(
                        mFriendlyName, activity = currentActivity,
                        mOriginalFriendlyName = mOriginalFriendlyName, mDeviceId = mDeviceId,
                        mPublishKey = mPublishKey, mSubscribeKey = mSubscribeKey
                    )
                }
            }
        }
    }
}

