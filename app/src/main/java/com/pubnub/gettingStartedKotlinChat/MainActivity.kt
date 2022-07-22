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
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.objects.PNObjectEventResult
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

val simpleDateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss")

class MainActivity : ComponentActivity() {

    //  Viewmodel for the chat pane state
    var chatViewModel: ChatViewModel = ChatViewModel()
    private lateinit var messageListState: LazyListState;

    //  This application hardcodes a single channel name for simplicity.  Typically you would use separate channels for each
    //  type of conversation, e.g. each 1:1 chat would have its own channel, named appropriately.
    val groupChatChannel = "group_chat"
    val LOG_TAG = "PNChatApp"
    private lateinit var deviceUuid: String
    private lateinit var pubnub: PubNub
    private lateinit var mlistener: Listener
    private var mLaunchingSettings: Boolean = false

    //  The settings screen is simplistic so startActivityForResult will suffice to control the app settings.
    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val friendlyName = data?.getStringExtra("friendly_name")
                replaceMemberName(deviceUuid, friendlyName.toString())
                Log.d(LOG_TAG, "Received onResult")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  Create a device-specific UUID to represent this device and user, so PubNub knows who is connecting.
        //  More info: https://support.pubnub.com/hc/en-us/articles/360051496532-How-do-I-set-the-UUID-
        //  All Android IDs are user-resettable but are still appropriate for use here.
        deviceUuid = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)

        //  Create PubNub configuration and instantiate the PubNub object, used to communicate with PubNub
        val config = PNConfiguration(UserId(deviceUuid)).apply {
            publishKey = BuildConfig.PUBLISH_KEY
            subscribeKey = BuildConfig.SUBSCRIBE_KEY
            logVerbosity = PNLogVerbosity.NONE
        }
        pubnub = PubNub(config)

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

                        //  The friendly name is used in place of the UUID.  E.g. the user's name.
                        startSettingsActivityIntent.putExtra(
                            "current_friendly_name",
                            resolveFriendlyName(chatViewModel, deviceUuid)
                        )
                        //  Provide the Settings activity with everything it needs to communicate with PubNub
                        //  Using the same credentials as the Main Activity.  The PubNub object is not
                        //  Parcelable or Serializable, so we need to create a new one.
                        startSettingsActivityIntent.putExtra("device_id", deviceUuid)
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
                            deviceID = deviceUuid, viewModel = chatViewModel,
                            messageListState = messageListState
                        )
                    }
                    //  Text field to input messages.  Also defines the 'Send' button'
                    MessageInput(
                        onSent =
                        { message ->
                            //  Code that executes whenever the 'Send' button is pressed
                            val metaInfo = JsonObject()

                            //  Attach our UUID as meta info to the message, this is useful in history to know who sent each message
                            metaInfo.addProperty("uuid", deviceUuid)

                            //  Publish the message to PubNub using the pre-defined channel for this group chat
                            pubnub.publish(
                                channel = groupChatChannel,
                                message = message,
                                meta = metaInfo
                            ).async { result, status ->
                                if (!status.error) {
                                    Log.v(
                                        LOG_TAG,
                                        "Message sent, timetoken: ${result!!.timetoken}"
                                    )
                                    //  Once the message has been published, handle the result.  In this case scroll the message list
                                    //  and log any errors.
                                    coroutineScope.launch {
                                        messageListState.animateScrollToItem(chatViewModel.messages.size)
                                    }
                                } else {
                                    Log.w(LOG_TAG, "Error while publishing")
                                    status.exception?.printStackTrace()
                                }
                            }
                        }, onChange = {})
                }
            }
        }

        //  When the application is first loaded, it is common to load any recent chat messages so the user
        //  can get caught up with conversations they missed.  Every application will handle this differently
        //  but here we just load the 12 most recent messages
        pubnub.history(
            channel = groupChatChannel,
            includeTimetoken = true,
            includeMeta = true,
            count = 12
        ).async { result, status ->
            result?.messages?.forEach { message ->
                if (!status.error) {
                    //  Recreate the message and add it to the viewModel for display.  The Message class is also defined
                    //  under ui.viewmodel
                    var newMsg = Message()
                    newMsg.message = message.entry.asString
                    try {
                        var metaInfo: JsonObject = message.meta as JsonObject
                        newMsg.senderUuid = metaInfo.get("uuid").asString
                    } catch (e: Exception) {
                    }
                    newMsg.timestamp = message.timetoken!!
                    chatViewModel.messages.add(newMsg)

                } else {
                    Log.w(LOG_TAG, "Error while retrieving history")
                }
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

        //  Subscribe to the pre-defined channel representing this chat group.  This will allow us to receive messages
        //  and presence events for the channel (what other users are in the room)
        pubnub.subscribe(
            channels = listOf(groupChatChannel),
            withPresence = true
        )

        //  Determine who is currently chatting in the channel.  I use an ArrayList in the viewModel to present this information
        //  on the UI, managed through a couple of addMember and removeMember methods
        //  I am definitely here
        addMember(deviceUuid)

        //  PubNub has an API to determine who is in the room.  Use this call sparingly since you are only ever likely to
        //  need to know EVERYONE in the room when the UI is first created.
        pubnub.hereNow(
            channels = listOf(groupChatChannel),
            includeState = true,
            includeUUIDs = true
        ).async { result, status ->
            if (!status.error) {
                //  The API will return an array of occupants in the channel, defined by their
                //  UUID.  This application will need to look up the friendly named defined for
                //  each of these UUIDs (later)
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
                    var newMsg = Message()
                    newMsg.message = pnMessageResult.message.asString
                    newMsg.senderUuid = pnMessageResult.publisher.toString()
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
                                addMember(userId);
                            }
                            pnPresenceEventResult.leave?.forEach { userId ->
                                removeMember(userId);
                            }
                        }
                    }
                }

            }

            //  Whenever Object meta data is changed, an Object event is recenved.
            //  See: https://www.pubnub.com/docs/chat/sdks/users/setup
            //  Use this to be notified when other users change their friendly names
            override fun objects(pubnub: PubNub, objectEvent: PNObjectEventResult) {
                Log.d(LOG_TAG, "objects callback")
                //  todo implement this to receive updates when others change their friendly name
                with(objectEvent.extractedMessage)
                {

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

        //  This getting started application is set up to unsubscribe from all channels when the app goes into the background.
        //  This is good to show the principles of presence but you don't need to do this in a production app if it does not fit your use case.
        pubnub.unsubscribe(
            channels = listOf(groupChatChannel)
        )
        pubnub.removeListener(mlistener)
    }

    //  The mapping of UUIDs to friendly names is kept in a hashmap in the viewModel.
    //  Return the friendly name for a given UUID
    private fun resolveFriendlyName(chatViewModel: ChatViewModel, uuid: String): String {
        if (chatViewModel.memberNames.containsKey(uuid))
            return chatViewModel.memberNames.get(uuid).toString()
        else
            return uuid
    }

    //  A UUID is present in the chat (as determined by either hereNow or the presence event)
    //  Update our chat member list
    fun addMember(uuid: String) {
        if (!chatViewModel.groupMemberUuids.contains(uuid))
            chatViewModel.groupMemberUuids.add(uuid)
        lookupMemberName(uuid)
    }

    //  A UUID is absent from the chat (as determined by either hereNow or the presence event)
    //  Update our chat member list
    fun removeMember(uuid: String) {
        if (chatViewModel.groupMemberUuids.contains(uuid))
            chatViewModel.groupMemberUuids.remove(uuid)
    }

    //  The 'master record' for each device's friendly name is stored in PubNub object storage.
    //  This avoids the application defining its own server storage or trying to keep track of all
    //  friendly names on every device.  Since PubNub Objects understand the concept of a user name
    //  (along with other common fields like email and profileUrl), it makes the process straight forward
    private fun lookupMemberName(uuid: String) {
        //  Resolve the friendly name of the UUID
        if (!chatViewModel.memberNames.containsKey(uuid)) {
            pubnub.getUUIDMetadata(
                uuid = uuid,
            ).async { result, status ->
                if (!status.error) {
                    //  Add the user's name to the memberNames hashmap (part of the viewModel, so
                    //  the UI will update accordingly)
                    chatViewModel.memberNames.put(uuid, result?.data?.name.toString())
                } else {
                    Log.w(LOG_TAG, "Error while 'getUUIDMetadata'")
                }
            }
        }
    }

    //  Update the hashmap of UUID --> friendly name mappings.
    //  Used for when names CHANGE
    private fun replaceMemberName(uuid: String, newName: String) {
        chatViewModel.memberNames.put(uuid, newName)
    }

}

