package com.pubnub.gettingStartedKotlinChat.ui.view

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pubnub.gettingStartedKotlinChat.simpleDateFormat
import com.pubnub.gettingStartedKotlinChat.ui.theme.GettingStartedChatTheme
import com.pubnub.gettingStartedKotlinChat.viewmodel.ChatViewModel
import com.pubnub.gettingStartedKotlinChat.viewmodel.Message
import com.pubnub.gettingStartedKotlinChat.R

object MainUI {

    private fun getDateString(time: Long): String = simpleDateFormat.format(time)
    private fun resolveMemberName(chatViewModel: ChatViewModel, deviceId: String): String {
        if (chatViewModel.memberNames.containsKey(deviceId))
            return chatViewModel.memberNames.get(deviceId).toString()
        else
            return deviceId
    }

    @Composable
    fun InformationBar(viewModel: ChatViewModel, onSettingsClick: () -> Unit) {
        GettingStartedChatTheme {
            Surface(
                color = MaterialTheme.colors.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(end = 20.dp)
                ) {
                    val memberDeviceIds = viewModel.groupMemberDeviceIds
                    var displayMembers = ""
                    memberDeviceIds.forEach { memberDeviceId ->
                        if (!displayMembers.equals(""))
                            displayMembers += ", "
                        var memberName = memberDeviceId
                        if (viewModel.memberNames.containsKey(memberDeviceId))
                            memberName = viewModel.memberNames.get(memberDeviceId).toString()
                        displayMembers += memberName
                    }
                    var mDisplayMenu by remember { mutableStateOf(false) }
                    Image(
                        painterResource(id = R.drawable.ic_outline_group_24),
                        contentDescription = "",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .height(70.dp)
                            .width(70.dp)
                    )
                    Column(modifier = Modifier.padding(start = 10.dp).fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .padding(0.dp)
                                .offset(0.dp, 10.dp)
                        )
                        {
                            Text(
                                text = viewModel.heading,
                                style = MaterialTheme.typography.h1,
                                fontSize = 30.sp
                            )
                        }
                        Row()
                        {
                            Text(
                                text = "Members Online: \n$displayMembers",
                                style = MaterialTheme.typography.body1,
                            )
                        }

                    }
                    Column(modifier = Modifier.defaultMinSize(100.dp))
                    {
                        IconButton(modifier = Modifier.requiredWidth(100.dp),
                            onClick = { mDisplayMenu = !mDisplayMenu }) {
                            Icon(Icons.Default.MoreVert, "")
                        }
                        DropdownMenu(
                            expanded = mDisplayMenu,
                            onDismissRequest = { mDisplayMenu = false },
                        ) {
                            DropdownMenuItem(onClick = { mDisplayMenu = false; onSettingsClick() }) {
                                Text(text = "Settings", style = MaterialTheme.typography.body1)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MessageList(deviceID: String, viewModel: ChatViewModel, messageListState: LazyListState) {
        LazyColumn(state = messageListState)
        {

            items(items = viewModel.messages, itemContent = { item ->
                MessageView(chatViewModel = viewModel, item = item, deviceID = deviceID)
            })

        }
    }


    @Composable
    fun MessageView(chatViewModel: ChatViewModel, item: Message, deviceID: String) {
        var messageAlignment = Alignment.Start
        if (item.senderDeviceId == deviceID) {
            //  Message we sent
            messageAlignment = Alignment.End
        }
        GettingStartedChatTheme {
            Column(horizontalAlignment = messageAlignment, modifier = Modifier.fillMaxWidth())
            {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(5.dp)
                        .border(2.dp, MaterialTheme.colors.primary, RoundedCornerShape(20))
                        .background(MaterialTheme.colors.primaryVariant, RoundedCornerShape(20))
                        .padding(12.dp)
                )
                {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                        .fillMaxWidth())
                    {
                        Image(
                            painter = painterResource(R.drawable.ic_outline_person_outline_24),
                            contentDescription = "",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .padding(end = 5.dp)
                                .height(30.dp)
                                .width(30.dp)
                                .background(Color.White, CircleShape)

                        )
                        Column()
                        {
                            Text(text = item.message, style = MaterialTheme.typography.body1)
                            Row()
                            {
                                Text(
                                    text = resolveMemberName(chatViewModel, item.senderDeviceId),
                                    style = MaterialTheme.typography.subtitle1,
                                    textAlign = TextAlign.Left
                                )
                                Text(
                                    text = getDateString(item.timestamp / 10000),
                                    style = MaterialTheme.typography.subtitle1,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                        }
                    }

                }

            }
        }
    }

    @Composable
    fun MessageInput(
        placeholder: String = "Type Message",
        initialText: String = "",
        onSent: (String) -> Unit,
        onChange: (String) -> Unit,
    ) {
        var text by rememberSaveable { mutableStateOf(initialText) }

        val sendAction: (String) -> Unit = {
            // reset input
            val message = it
            text = ""

            onSent(message)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.background
        ) {
            Row(modifier = Modifier.fillMaxWidth()/*modifier = theme.modifier*/) {
                TextField(
                    textStyle = MaterialTheme.typography.body1,
                    value = text,
                    onValueChange = { text = it.filter { !(it == '\n') }; onChange(it); },
                    placeholder = { Text(placeholder, style = MaterialTheme.typography.body1) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "message_input_text" },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(onSend = { sendAction(text) }),
                    singleLine = true,
                    trailingIcon = {
                        Row {
                            SendButton(
                                enabled = text.isNotBlank(),
                                action = { sendAction(text) },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun SendButton(enabled: Boolean = true, action: () -> Unit) {
        Button(
            onClick = action,
            enabled = enabled,
        ) {
            Text(
                text = "Send",
                style = MaterialTheme.typography.body1
            )
        }
    }


}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun DefaultPreviewMessageList() {
    val sampleMessage = Message()
    sampleMessage.message = "This is a Test Message"
    sampleMessage.senderDeviceId = "123"
    sampleMessage.timestamp = 16584196619030000L
    val sampleViewModel = ChatViewModel()
    sampleViewModel.memberNames.put("123", "Device 123")
    sampleViewModel.messages = mutableStateListOf(sampleMessage)
    val messageListState = rememberLazyListState()
    MainUI.MessageList("123", sampleViewModel, messageListState)
}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun HeadingPreviewShort() {
    val sampleViewModel = ChatViewModel()
    sampleViewModel.heading = "Group Chat"
    sampleViewModel.memberNames.put("123", "Device 123")
    sampleViewModel.groupMemberDeviceIds = mutableStateListOf("123")

    MainUI.InformationBar(sampleViewModel, {})
}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun HeadingPreviewLong() {
    val sampleViewModel = ChatViewModel()
    sampleViewModel.heading = "Group Chat"
    sampleViewModel.memberNames.put("123", "Device 123alsidjhsd asdfha sdfh askjdfh aslkjdhf ashdf askhjd fasuh dfasudh fashd flakshd faksl dhff oaishasdfasdfdfo asidd")
    sampleViewModel.groupMemberDeviceIds = mutableStateListOf("123")

    MainUI.InformationBar(sampleViewModel, {})
}
