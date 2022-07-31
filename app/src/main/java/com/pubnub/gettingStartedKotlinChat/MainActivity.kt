package com.pubnub.gettingStartedKotlinChat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.google.gson.JsonObject
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.UserId
import com.pubnub.api.callbacks.Listener
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNLogVerbosity
import com.pubnub.api.models.consumer.PNBoundedPage
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.history.PNFetchMessageItem
import com.pubnub.api.models.consumer.objects.membership.PNChannelMembership
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.objects.PNObjectEventResult
import com.pubnub.api.models.consumer.pubsub.objects.PNSetUUIDMetadataEventMessage
import com.pubnub.gettingStartedKotlinChat.ui.theme.GettingStartedChatTheme
import com.pubnub.gettingStartedKotlinChat.ui.view.MainUI.InformationBar
import com.pubnub.gettingStartedKotlinChat.ui.view.MainUI.MessageInput
import com.pubnub.gettingStartedKotlinChat.ui.view.MainUI.MessageList
import com.pubnub.gettingStartedKotlinChat.viewmodel.ChatViewModel
import com.pubnub.gettingStartedKotlinChat.viewmodel.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val simpleDateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.US)

class MainActivity : ComponentActivity() {

    //  Viewmodel for the chat pane state
    var chatViewModel: ChatViewModel = ChatViewModel()
    private lateinit var messageListState: LazyListState

    //  This application hardcodes a single channel name for simplicity.  Typically you would use separate channels for each
    //  type of conversation, e.g. each 1:1 chat would have its own channel, named appropriately.
    val groupChatChannel = "group_chat"
    val LOG_TAG = "PNChatApp"
    private lateinit var deviceId: String
    private lateinit var pubnub: PubNub
    private lateinit var mlistener: Listener
    private var mLaunchingSettings: Boolean = false

    //  The settings screen is simplistic so startActivityForResult will suffice to control the app settings.
    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val friendlyName = data?.getStringExtra("friendly_name")
                //  The below line duplicates functionality from the object event callback but if the user
                //  does not set 'user metadata events' in the portal, at least we update our own member name.
                replaceMemberName(deviceId, friendlyName.toString())
                Log.v(LOG_TAG, "Received onResult")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  TUTORIAL: STEP 3A CODE GOES HERE

        //  You need to specify a Publish and Subscribe key when configuring PubNub on the device.
        //  This application will load them from your gradle.properties file (See ReadMe for information on obtaining keys)
        if (BuildConfig.PUBLISH_KEY.startsWith("REPLACE") || BuildConfig.SUBSCRIBE_KEY.startsWith("REPLACE")) {
            chatViewModel.heading = "MISSING PUBNUB KEYS"
        }

        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        //  The application UI is kept deliberately simple for readability.
        //  See ui.view.MainUI.kt for definitions of visual components.
        setContent {
            messageListState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            GettingStartedChatTheme {
                Column {

                    //  Header to hold the current friendly name of the device along with other devices in the group chat
                    //  The below logic will launch the settings activity when the option is selected
                    //  See SettingsActivity.kt
                    InformationBar(chatViewModel, onSettingsClick = {
                        val startSettingsActivityIntent =
                            Intent(applicationContext, SettingsActivity::class.java)

                        //  The friendly name is used in place of the DeviceId.  E.g. the user's name.
                        startSettingsActivityIntent.putExtra(
                            "current_friendly_name",
                            resolveFriendlyName(chatViewModel, deviceId)
                        )
                        //  Provide the Settings activity with everything it needs to communicate with PubNub
                        //  Using the same credentials as the Main Activity.  The PubNub object is not
                        //  Parcelable or Serializable, so we need to create a new one.
                        startSettingsActivityIntent.putExtra("device_id", deviceId)
                        startSettingsActivityIntent.putExtra(
                            "subscribe_key",
                            BuildConfig.SUBSCRIBE_KEY
                        )
                        startSettingsActivityIntent.putExtra(
                            "publish_key",
                            BuildConfig.PUBLISH_KEY
                        )
                        resultLauncher.launch(startSettingsActivityIntent)
                        mLaunchingSettings = true
                    })

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        color = MaterialTheme.colors.background
                    ) {
                        //  List of messages received in the group
                        MessageList(
                            deviceID = deviceId, viewModel = chatViewModel,
                            messageListState = messageListState
                        )
                    }
                    //  Text field to input messages.  Also defines the 'Send' button'
                    MessageInput(
                        onSent =
                        { message ->
                            //  Code that executes whenever the 'Send' button is pressed

                            //  TUTORIAL: STEP 3C CODE GOES HERE

                        }, onChange = {})
                }
            }
        }

        //  In order to receive object UUID events (in the addListener) it is required to set our
        //  membership using the Object API.
        pubnub.setMemberships(
            channels = listOf(
                PNChannelMembership.Partial(channelId = groupChatChannel)
            )
        ).async { result, status ->
            if (!status.error) {
                Log.v(LOG_TAG, "Success while executing setMemberships")
            } else {
                Log.w(LOG_TAG, "Error while executing setMemberships")
            }
        }

        //  When the application is first loaded, it is common to load any recent chat messages so the user
        //  can get caught up with conversations they missed.  Every application will handle this differently
        //  but here we just load the 8 most recent messages
        pubnub.fetchMessages(
            channels = listOf(groupChatChannel),
            page = PNBoundedPage(limit = 8),
            includeMeta = true,
            includeUUID = true
        ).async { result, status ->
            if (!status.error) {
                result!!.channels.forEach { (channel, messages) ->
                    if (channel == groupChatChannel) {
                        messages.forEach { messageItem: PNFetchMessageItem ->
                            val newMsg = Message()
                            newMsg.message = messageItem.message.asString
                            newMsg.timestamp = messageItem.timetoken
                            newMsg.senderDeviceId = messageItem.uuid.toString()
                            chatViewModel.messages.add(newMsg)

                            lookupMemberName(newMsg.senderDeviceId)
                        }
                    }
                }
            } else {
                Log.w(LOG_TAG, "Error while retrieving history")
            }
        }
    }

    //  This application is designed to unsubscribe from the channel when it goes to the background and re-subscribe
    //  when it comes to the foreground.  This is a fairly common design pattern.  In production, you would probably
    //  also use a native push message to alert the user whenever there are missed messages.  For more information
    //  see https://www.pubnub.com/tutorials/push-notifications/
    override fun onResume() {
        super.onResume()

        if (mLaunchingSettings) {
            mLaunchingSettings = false
            return
        }

        //  TUTORIAL: STEP 3B CODE GOES HERE (1/2)

        //  Determine who is currently chatting in the channel.  I use an ArrayList in the viewModel to present this information
        //  on the UI, managed through a couple of addMember and removeMember methods
        //  I am definitely here
        addMember(deviceId)

        //  PubNub has an API to determine who is in the room.  Use this call sparingly since you are only ever likely to
        //  need to know EVERYONE in the room when the UI is first created.
        pubnub.hereNow(
            channels = listOf(groupChatChannel),
            includeUUIDs = true
        ).async { result, status ->
            if (!status.error) {
                //  The API will return an array of occupants in the channel, defined by their
                //  ID.  This application will need to look up the friendly name defined for
                //  each of these IDs (later)
                result?.channels?.get(groupChatChannel)?.occupants?.forEach { i ->
                    addMember(i.uuid)
                }
            } else {
                Log.w(LOG_TAG, "Error while executing hereNow")
            }
        }

        //  Applications receive various types of information from PubNub through a 'listener'
        //  This application dynamically registers a listener when it comes to the foreground
        val listener = object : SubscribeCallback() {

            //  It is common for an application to receive status events in production, e.g. to
            //  be notified that a message has been successfully published.  For simplicity, this
            //  is omitted from this app.
            override fun status(pubnub: PubNub, pnStatus: PNStatus) {
                //  Not used
            }

            //  A message is received from PubNub.  This is the entry point for all messages on all
            //  channels or channel groups, though this application only uses a single channel.
            override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
                if (pnMessageResult.channel == groupChatChannel) {
                    Log.v(LOG_TAG, "Received message ${pnMessageResult.message}")
                    //  Display the message by adding it to the viewModel
                    val newMsg = Message()
                    newMsg.message = pnMessageResult.message.asString
                    newMsg.senderDeviceId = pnMessageResult.publisher.toString()
                    newMsg.timestamp = pnMessageResult.timetoken!!
                    chatViewModel.messages.add(newMsg)

                    CoroutineScope(Dispatchers.Main).launch {
                        messageListState.scrollToItem(chatViewModel.messages.size - 1)
                    }
                }
            }

            //  Be notified that a 'presence' event has occurred.  I.e. somebody has left or joined
            //  the channel.  This is similar to the earlier hereNow call but this API will only be
            //  invoked when presence information changes, meaning you do NOT have to call hereNow
            //  periodically.  More info: https://www.pubnub.com/docs/sdks/kotlin/api-reference/presence
            override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {
                when (pnPresenceEventResult.event) {
                    "join" -> {
                        addMember(pnPresenceEventResult.uuid.toString())
                    }
                    "leave" -> {
                        removeMember(pnPresenceEventResult.uuid.toString())
                    }
                    "interval" -> {
                        //  'join' and 'leave' will work up to the ANNOUNCE_MAX setting (defaults to 20 users)
                        //  Over ANNOUNCE_MAX, an 'interval' message is sent.  More info: https://www.pubnub.com/docs/presence/presence-events#interval-mode
                        //  The below logic requires that 'Presence Deltas' be defined for the keyset, you can do this from the admin dashboard
                        if (pnPresenceEventResult.join?.size == 0 && pnPresenceEventResult.leave?.size == 0) {
                            Log.v(
                                LOG_TAG,
                                "No change since last interval update.  This is probably normal but could also mean you have not enabled presence deltas on your key"
                            )
                        } else {
                            pnPresenceEventResult.join?.forEach { userId ->
                                addMember(userId)
                            }
                            pnPresenceEventResult.leave?.forEach { userId ->
                                removeMember(userId)
                            }
                        }
                    }
                }

            }

            //  Whenever Object meta data is changed, an Object event is received.
            //  See: https://www.pubnub.com/docs/chat/sdks/users/setup
            //  Use this to be notified when other users change their friendly names
            override fun objects(pubnub: PubNub, objectEvent: PNObjectEventResult) {
                if (objectEvent.extractedMessage.type.equals("uuid")) {
                    Log.d(LOG_TAG, "objects callback with " + objectEvent.extractedMessage)
                    val extractedMessage: PNSetUUIDMetadataEventMessage =
                        objectEvent.extractedMessage as PNSetUUIDMetadataEventMessage
                    replaceMemberName(
                        extractedMessage.data.id,
                        extractedMessage.data.name.toString()
                    )
                }
            }
        }

        //  Having created the listener object, add it to the PubNub object and remember it so it can be removed during onPause()
        pubnub.addListener(listener)
        mlistener = listener
    }

    override fun onPause() {
        super.onPause()
        if (mLaunchingSettings)
            return

        //  TUTORIAL: STEP 3B CODE GOES HERE (2/2)

        pubnub.removeListener(mlistener)
    }

    //  The mapping of device IDs to friendly names is kept in a hashmap in the viewModel.
    //  Return the friendly name for a given Device Id
    private fun resolveFriendlyName(chatViewModel: ChatViewModel, deviceId: String): String {
        if (chatViewModel.memberNames.containsKey(deviceId))
            return chatViewModel.memberNames.get(deviceId).toString()
        else
            return deviceId
    }

    //  A DeviceID is present in the chat (as determined by either hereNow or the presence event)
    //  Update our chat member list
    fun addMember(deviceId: String) {
        if (!chatViewModel.groupMemberDeviceIds.contains(deviceId))
            chatViewModel.groupMemberDeviceIds.add(deviceId)
        lookupMemberName(deviceId)
    }

    //  A Device ID is absent from the chat (as determined by either hereNow or the presence event)
    //  Update our chat member list
    fun removeMember(deviceId: String) {
        if (chatViewModel.groupMemberDeviceIds.contains(deviceId))
            chatViewModel.groupMemberDeviceIds.remove(deviceId)
    }

    //  The 'master record' for each device's friendly name is stored in PubNub Object storage.
    //  This avoids the application defining its own server storage or trying to keep track of all
    //  friendly names on every device.  Since PubNub Objects understand the concept of a user name
    //  (along with other common fields like email and profileUrl), it makes the process straight forward
    private fun lookupMemberName(deviceId: String) {
        //  Resolve the friendly name of the DeviceId
        if (!chatViewModel.memberNames.containsKey(deviceId)) {
            pubnub.getUUIDMetadata(
                uuid = deviceId,
            ).async { result, status ->
                if (!status.error) {
                    //  Add the user's name to the memberNames hashmap (part of the viewModel, so
                    //  the UI will update accordingly)
                    chatViewModel.memberNames.put(deviceId, result?.data?.name.toString())
                } else {
                    Log.w(LOG_TAG, "Error while 'getUUIDMetadata'")
                }
            }
        }
    }

    //  Update the hashmap of DeviceId --> friendly name mappings.
    //  Used for when names CHANGE
    private fun replaceMemberName(deviceId: String, newName: String) {
        chatViewModel.memberNames.put(deviceId, newName)
    }

}

