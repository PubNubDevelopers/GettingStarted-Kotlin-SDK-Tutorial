package com.pubnub.gettingStartedKotlinChat.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel

//  Viewmodel for the chat view, including heading and message list
class ChatViewModel : ViewModel() {
    var heading: String by mutableStateOf("Group Chat")
    var messages = mutableStateListOf<Message>()
    var groupMemberUuids = mutableStateListOf<String>()
    var memberNames = mutableStateMapOf<String, String>()

}

