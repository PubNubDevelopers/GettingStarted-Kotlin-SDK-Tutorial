package com.pubnub.gettingStartedKotlinChat.ui.view

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.UserId
import com.pubnub.gettingStartedKotlinChat.ui.theme.GettingStartedChatTheme
import com.pubnub.gettingStartedKotlinChat.ui.theme.PubNubLightGreen

object SettingsUI {

    @Composable
    fun SettingsHeader() {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.primary
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Settings", style = MaterialTheme.typography.h1, fontSize = 30.sp)
            }
        }
    }

    @Composable
    fun SettingsWelcomeText() {
        Text(
            text = "Specify a friendly name to use instead of the device hardware ID",
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
        )
    }

    @Composable
    fun SettingsOptionFriendlyName(
        mFriendlyName: String, activity: Activity,
        mOriginalFriendlyName: String, mDeviceId: String,
        mPublishKey: String, mSubscribeKey: String
    ) {
        var text by remember { mutableStateOf(TextFieldValue(mFriendlyName)) }
        Row(verticalAlignment = Alignment.CenterVertically)
        {
            Surface()
            {
                Column(modifier = Modifier.padding(5.dp))
                {
                    TextField(
                        value = text,
                        onValueChange = { newText ->
                            text = newText
                        },
                        label = {
                            Text(
                                text = "Friendly Device Name",
                                style = MaterialTheme.typography.subtitle1
                            )
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = PubNubLightGreen
                        ),
                    )
                }
            }
            Surface(
                modifier = Modifier.padding(end = 10.dp)
            )
            {
                Button(onClick = {
                    //  User has pressed the 'Save' button.  Do two things:
                    //  1. Return the new friendly name to the Main Activity (this is just for convenience).  Note: we were invoked with StartActivityForResult
                    //  2. Persist the friendly name in PubNub object storage (this is the master record)
                    var returnIntent = Intent()
                    returnIntent.putExtra("friendly_name", text.text)
                    activity.setResult(Activity.RESULT_OK, returnIntent)

                    if (!mOriginalFriendlyName.equals(text.text)) {
                        val LOG_TAG = "PNChatApp"
                        //  Create a PubNub object from scratch to handle the communication from
                        //  this activity since the pubnub object is neither serializable or parcelable
                        //  Could probably have done this on a separate thread but keeping this demo simple
                        //  and it does not appear to introduce a noticeable delay to the button push.
                        val config = PNConfiguration(UserId(mDeviceId)).apply {
                            publishKey = mPublishKey
                            subscribeKey = mSubscribeKey
                        }
                        //  Mapping of UUID to friendly name master record is PubNub object storage
                        //  https://www.pubnub.com/docs/sdks/kotlin/api-reference/objects
                        var pubnub = PubNub(config)
                        pubnub.setUUIDMetadata(
                            name = text.text,
                            includeCustom = true
                        ).async { result, status ->
                            if (status.error) {
                                Log.w(
                                    LOG_TAG,
                                    "Error setting UUID Metadata.  Status code: " + status.statusCode
                                )
                            } else if (result != null) {
                                Log.d(
                                    LOG_TAG,
                                    "UUID Metadata successfully set to " + result.data?.name
                                )
                            }
                        }
                    }
                    //  Return focus back to the main activity
                    activity.finish()
                }) {
                    Text(text = "Save", style = MaterialTheme.typography.body1)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    GettingStartedChatTheme {
        //SettingsHeader()
        SettingsUI.SettingsOptionFriendlyName(
            "Device 123", Activity(),
            "Friendly Name", "123", "123", "123"
        )
    }
}
