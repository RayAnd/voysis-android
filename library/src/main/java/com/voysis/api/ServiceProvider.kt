package com.voysis.api

import android.content.Context
import com.google.gson.Gson
import com.voysis.client.provider.ClientProvider
import com.voysis.client.provider.CloudClientProvider
import com.voysis.client.provider.TfLiteModelProvider
import com.voysis.generateAudioWavRecordParams
import com.voysis.generateDefaultAudioWavRecordParams
import com.voysis.generateOkHttpClient
import com.voysis.getHeaders
import com.voysis.model.request.Token
import com.voysis.recorder.AudioRecorder
import com.voysis.recorder.AudioRecorderImpl
import com.voysis.sevice.CloudTokenManager
import com.voysis.sevice.Converter
import com.voysis.sevice.ServiceImpl
import com.voysis.wakeword.DetectorType
import com.voysis.wakeword.WakeWordDetectorImpl
import com.voysis.wakeword.WakeWordServiceImpl
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
     * @param context context android context
     * @param config config
     * @param okClient optional okhttp client
     * @param audioRecorder optional to override default recorder
     * @return new `Service` instance
     */
    @JvmOverloads
    fun makeCloud(context: Context,
                  config: Config,
                  okClient: OkHttpClient? = null,
                  audioRecorder: AudioRecorder? = null): Service {
        val converter = Converter(getHeaders(context), Gson())
        val tokenManager = CloudTokenManager(config.refreshToken)
        return make(context, CloudClientProvider(config, generateOkHttpClient(okClient), converter), config, tokenManager, audioRecorder, converter)
    }

    /**
     * @param context context android context
     * @param config config
     * @param audioRecorder optional to override default recorder
     * @return new `Service` instance
     */
    fun makeCloud(context: Context,
                  config: Config,
                  audioRecorder: AudioRecorder? = null): Service {
        return makeCloud(context, config, null, audioRecorder)
    }

    /**
     * This method will block the current thread until the models are loaded
     *
     * @param context context android context
     * @param config config
     * @param audioRecorder optional to override default local recorder
     * @throws ClassNotFoundException if it does not find its dependencies
     *
     * @return new local execution `Service`
     */
    @Throws(ClassNotFoundException::class)
    fun makeLocal(context: Context,
                  config: BaseConfig,
                  audioRecorder: AudioRecorder? = AudioRecorderImpl(generateAudioWavRecordParams(config))): Service {
        val localTokenManager = object : TokenManager {
            override val refreshToken: String = REFRESH_TOKEN
            override val token: String = LOCAL_TOKEN
            override var sessionToken: Token? = Token(NO_EXPIRE, token)
            override fun tokenIsValid(): Boolean {
                return true
            }
        }
        val clientProviderClass = Class.forName("com.voysis.client.provider.LocalClientProvider")
        val clientProviderConstructor = clientProviderClass.getConstructor(Context::class.java, BaseConfig::class.java, AudioRecorder::class.java)
        val clientProviderConstructorInstance = clientProviderConstructor.newInstance(context, config, audioRecorder) as ClientProvider
        return make(context, clientProviderConstructorInstance, config, localTokenManager, audioRecorder!!)
    }

    private fun make(context: Context,
                     clientProvider: ClientProvider,
                     config: BaseConfig,
                     tokenManager: TokenManager,
                     recorder: AudioRecorder? = null,
                     converter: Converter = Converter(getHeaders(context), Gson())): Service {
        val client = clientProvider.createClient()
        val audioRecorder = recorder ?: AudioRecorderImpl(generateAudioWavRecordParams(config))
        val service = ServiceImpl(client, audioRecorder, converter, config.userId, tokenManager)
        return when (config.serviceType) {
            ServiceType.WAKEWORD -> {
                val wakeWordDetector = makeWakeWordDetector(context, config.resourcePath!!, DetectorType.SINGLE, audioRecorder)
                return WakeWordServiceImpl(wakeWordDetector, service)
            }
            else -> service
        }
    }

    /**
     * This method will create a wakeword detector provided the wakeword.tflite
     * model is at the path provided. Note: path must exist within assets directory
     *
     * @param context context android context
     * @param path path to wakeword model
     * @throws type DetectorType
     *
     * @return new `WakeWordDetectorImpl`
     */
    fun makeWakeWordDetector(context: Context,
                             path: String,
                             type: DetectorType,
                             recorder: AudioRecorder = AudioRecorderImpl(generateDefaultAudioWavRecordParams())): WakeWordDetectorImpl {
        val interpreter = TfLiteModelProvider(path, context.assets).createTfLiteInterpreter("wakeword.tflite")
        return WakeWordDetectorImpl(recorder, interpreter, type)
    }
}
