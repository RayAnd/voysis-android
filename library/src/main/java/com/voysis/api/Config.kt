package com.voysis.api

import com.voysis.recorder.AudioRecordParams
import java.net.URL

interface Config {

    /**
     * @return refreshToken
     */
    val refreshToken: String

    val userId: String?

    /**
     * Voice Activity Detection will automatically detect when the user stops speaking and
     * process the audio request.
     *
     * Note: setting this to true will cause the client to use a webSocket as opposed to a rest
     * interface under the hood
     * @return isVadEnabled boolean. default set to true.
     */
    val isVadEnabled: Boolean

    /**
     * @return url used by client for making audio requests.
     */
    val url: URL

    /**
     * Configurable audio parameters object used by underlying AudioRecord object.
     * If left blank the AudioRecord object will be created using default parameters.
     * @note It is recommended to leave this object blank unless non default values are
     * explicitly required by a specific use case.
     * @return audioRecordParams
     */
    val audioRecordParams: AudioRecordParams?
}
