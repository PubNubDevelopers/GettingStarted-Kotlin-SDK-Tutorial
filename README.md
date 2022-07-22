# Tutorial: Getting Started Developing a Chat app with the Kotlin SDK
> Simple chat app to demonstrate the basic principles of creating a chat app with PubNub.  This app is written in Kotlin, targeting Android and uses the JetPack Compose library for the UI

PubNub allows you to create chat apps from scratch or add them to your existing applications. You can focus on creating the best user experience while PubNub takes care of scalability, reliability, security, and global legislative compliance.

Create 1:1 private chat rooms, group chats, or mega chats for large scale events, for a variety of use cases.

> For the sake of simplicity, this application will only focus on a single 'group chat' room

![Screenshot](https://raw.githubusercontent.com/PubNubDevelopers/GettingStarted-Kotlin-SDK-Tutorial/main/media/screenshot01_small.png)

## Demo

This application is not available on the Play Store but can be built and run locally in an emulator (or multiple emulators to simulate chat between multiple participants)

## Features

- [Publish and Subscibe](https://www.pubnub.com/docs/sdks/kotlin/api-reference/publish-and-subscribe) for messages with the PubNub Kotlin SDK
- Use [Presence](https://www.pubnub.com/docs/sdks/kotlin/api-reference/presence) APIs to determine who is currently chatting
- The [History](https://www.pubnub.com/docs/sdks/kotlin/api-reference/storage-and-playback#history) API will retrieve past messages for users newly joining the chat
- Assign a 'friendly name' to yourself which will be available to others via the PubNjub [Object](https://www.pubnub.com/docs/sdks/kotlin/api-reference/objects) storage APIs

## Installing / Getting Started

To run this project yourself you will need a PubNub account

### Requirements
- [Android Studio](https://developer.android.com/studio).  Any recent version will work but this app was initially created with Android Studio Chipmunk.
- [PubNub Account](https://admin.pubnub.com/) (*Free*)

<a href="https://dashboard.pubnub.com/signup">
	<img alt="PubNub Signup" src="https://i.imgur.com/og5DDjf.png" width=260 height=97/>
</a>

### Get Your PubNub Keys

1. You’ll first need to sign up for a [PubNub account](https://dashboard.pubnub.com/signup/). Once you sign up, you can get your unique PubNub keys from the [PubNub Developer Portal](https://admin.pubnub.com/).

1. Sign in to your [PubNub Dashboard](https://admin.pubnub.com/).

1. Click Apps, then **Create New App**.

1. Give your app a name, and click **Create**.

1. Click your new app to open its settings, then click its keyset.

1. Enable the Presence feature for your keyset.  **Also tick the box for 'Presence Deltas'**

1. Enable the Stream Controller feature for your keyset.

1. Enable the Persistence feature for your keyset

1. Enable the Objects feature for your keyset.

1. Copy the Publish and Subscribe keys and paste them into your app as specified in the next step.

### Building and Running

1. You'll need to run the following commands from your terminal

1. Clone the Github repository

```bash
git clone https://github.com/PubNubDevelopers/GettingStarted-Kotlin-SDK-Tutorial.git
```

1. Navigate to the application directory

cd GettingStarted-Kotlin-SDK-Tutorial

1. Add your pub/sub keys to `gradle.properties`

1. Open the application in Android Studio and run on device or with an emulator.  Use multiple devices / emulators to simulate a more realistic chat experience.

## Contributing
Please fork the repository if you'd like to contribute. Pull requests are always welcome. 

## Links

Checkout the following links for more information on developing chat solutions with PubNub:

- Chat Real-Time Developer Path: https://www.pubnub.com/developers/chat-real-time-developer-path/
- Tour of PubNub features: https://www.pubnub.com/tour/introduction/
- Chat use cases with PubNub: https://www.pubnub.com/use-case/in-app-chat/
