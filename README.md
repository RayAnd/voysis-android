WARNING: This is a beta release of the Voysis Android SDK.

Voysis Android Kotlin SDK
=====================


This document provides a brief overview of the Voysis Android SDK.
This is an Android library that facilitates sending voice
queries to a Voysis instance. The SDK streams audio from the device microphone 
to the Voysis backend servers when called by the client application.


Documentation
-------------


The full documentation for the voysis api can be found here: [Voysis Developer Documentation](https://developers.voysis.com/docs)


Requirements
-------------
*minSdkVersion 19*


Overview
-------------


The `Service` class is the main interface used to process voice recognition requests.
It is accessed via the `ServiceProvider().make(context , config)` method. See demo application for more details.
The sdk supports both REST and Websocket connections depending on how the config file is setup.
*NOTE*: It is preferable to use the Websocket connection as it offers more flexibility. 
The sdk uses `Okhttp` under the hood for network connections.


Config 
-------------

To create a service instance you need to provide config information at construction time. 
The config file is comprised of several fields.

- **isVadEnabled** - *Boolean:* [VAD] stands for Voice Activation Detection. 
If set to true the library will automatically detect when the user has stopped speaking and process requests. 
This is made possible via an `okHttp` Websocket connection. See [Websocket docs](https://developers.voysis.com/docs/websocket-api) for more info on our Websocket api.
If turned off the library will default to a REST interface and the user will need to manually call `service.finish()` to process requests. See [REST docs](https://developers.voysis.com/docs/rest-api) for more info on our REST api.
*NOTE*: It is preferable to have `isVadEnabled` set to `true` as the Websocket provides the user with more functionality and flexibility.

- **url** - *URL:* This is the endpoint that network requests will be executed against. 
The url is provided when your Voysis service is delivered. To sign up for a Voysis service visit our [homepage](https://voysis.com/)   

- **refreshToken** - *String:* The refresh token is used for authenticating unique users and for refreshing session tokens. 
This is all taken care of from within the library once the `refreshToken` is provided. 
For information on how to generate a refresh token see [here](https://developers.voysis.com/docs/authorization#section-introduction)

Usage
-------------


- The first step is to create a `Service` instance using the android context and config object.
```kotlin
 val config = DataConfig(isVadEnabled = true, url = URL("INSERT_URL"), refreshToken = "INSERT_TOKEN")
 val service = ServiceProvider().make(context, config)
```


- Next: to make a request, call `service.startAudioQuery` with the mandatory `Callback` parameter and optional voysis `Context` (See context section below for details).
 
 **Note:** This call issues a network request and should not be done on the main thread. Note also that callbacks may not occur on the main thread.
```kotlin
class ExampleActivity : AppCompatActivity(), Callback {

    private fun onClick() {
        service.startAudioQuery(context = context, callback = this)
    }

    override fun success(response: StreamResponse) {
        // Mandatory: called when final response returned from server.
    }

    override fun failure(error: VoysisException) {
        // Mandatory: called when any error occurs
    }

    override fun recordingStarted() {
        //Optional: called when microphone begins recording.
    }

    override fun queryResponse(query: QueryResponse) {
        //Optional: called when successful connection is made to the server.
    }

    override fun recordingFinished(reason: FinishedReason) {
        //Optional: called when recording stops. Includes finishedReason enum.
    }
    
    override fun audioData(buffer: ByteBuffer) {
        //Optional: returns audio data to the user that can be used generating for dynamic animations, analytics etc.
    }
}

```

Voysis Context
-----------------

One of the features of the Voysis service is that it can use the `StreamResponse.context` 
(Not to be confused with Android [context](https://developer.android.com/reference/android/content/Context)) to refine and improve subsequent requests. In order to avail of this 
the developer must store the `context` response from the `AudioStreamResponse` and send it in the following `startAudioQuery(context , callback)` request.  

Integration Gradle
-------------


Step 1. Add the JitPack repository to your build file. Add it in your root `build.gradle` at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.voysis:voysis-android:1.x'
	}
	

Local Build
-------------


To build the project locally, clone the project and open the root directory in Android Studio.

	
IMPORTANT NOTE
-------------


The user must accept Microphone permissions prior to using this library. Failing to do so will result in an error.





