package com.voysis.api

import android.content.Context
import com.google.gson.Gson
import com.voysis.client.provider.LocalModelAssetProvider
import com.voysis.generateAudioRecordParams
import com.voysis.generateAudioWavRecordParams
import com.voysis.generateOkHttpClient
import com.voysis.getHeaders
import com.voysis.model.request.Token
import com.voysis.recorder.AudioRecorder
import com.voysis.recorder.AudioRecorderImpl
import com.voysis.sevice.Converter
import com.voysis.sevice.ServiceImpl
import com.voysis.sevice.CloudTokenManager
import com.voysis.client.provider.ClientProvider
import com.voysis.client.provider.CloudClientProvider
import okhttp3.OkHttpClient

/**
 * The Voysis.ServiceProvider is the sdk's primary object for making Voysis.Service instances.
 */
class ServiceProvider {
    companion object {
        private const val REFRESH_TOKEN = "LOCAL_REFRESH_TOKEN"
        private const val LOCAL_TOKEN = "LOCAL_TOKEN"
        private const val NO_EXPIRE = "NO_EXPIRE"
    }

    /**
     * @param config config
     * @param okClient optional okhttp client
     * @param audioRecorder optional to override default recorder
     * @return new `Service` instance
     */
    @JvmOverloads
    fun make(context: Context,
             config: Config,
             okClient: OkHttpClient? = null,
             audioRecorder: AudioRecorder = AudioRecorderImpl(generateAudioRecordParams(context, config))): Service {
        val converter = Converter(getHeaders(context), Gson())
        val tokenManager = CloudTokenManager(config.refreshToken)
        return make(context, CloudClientProvider(config, generateOkHttpClient(okClient), converter), config, tokenManager, audioRecorder, converter)
    }

    /**
     * @param config config
     * @param audioRecorder optional to override default recorder
     * @return new `Service` instance
     */
    fun make(context: Context,
             config: Config,
             audioRecorder: AudioRecorder = AudioRecorderImpl(generateAudioRecordParams(context, config))): Service {
        return make(context, config, null, audioRecorder)
    }

    /**
     * This method will block the current thread until the models are loaded
     *
     * @param config config
     * @param audioRecorder optional to override default local recorder
     * @throws ClassNotFoundException if it does not find its dependencies
     *
     * @return new local execution `Service`
     */
    @Throws(ClassNotFoundException::class)
    fun makeLocal(context: Context,
                  config: Config,
                  audioRecorder: AudioRecorder = AudioRecorderImpl(generateAudioWavRecordParams(config))
    ): Service {
        val localTokenManager = object : TokenManager {
            override val refreshToken: String = REFRESH_TOKEN
            override val token: String = LOCAL_TOKEN
            override var sessionToken: Token? = Token(NO_EXPIRE, token)
            override fun tokenIsValid(): Boolean {
                return true
            }
        }
        val clientProviderClass = Class.forName("com.voysis.client.provider.LocalClientProvider")
        val resourcesPath = LocalModelAssetProvider(context).extractModel("localResources")
        val clientProviderConstructor = clientProviderClass.getConstructor(resourcesPath::class.java, Config::class.java, AudioRecorder::class.java)
        val clientProviderConstructorInstance = clientProviderConstructor.newInstance(resourcesPath, config, audioRecorder) as ClientProvider
        return make(context, clientProviderConstructorInstance, config, localTokenManager, audioRecorder)
    }

    private fun make(context: Context,
                     clientProvider: ClientProvider,
                     config: Config,
                     tokenManager: TokenManager,
                     audioRecorder: AudioRecorder = AudioRecorderImpl(generateAudioWavRecordParams(config)),
                     converter: Converter = Converter(getHeaders(context), Gson())
    ): Service {
        val client = clientProvider.createClient()
        return ServiceImpl(client, audioRecorder, converter, config.userId, tokenManager)
    }
}
