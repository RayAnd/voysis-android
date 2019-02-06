package com.voysis.sevice

import com.voysis.api.Config
import com.voysis.recorder.AudioRecordParams
import java.net.URL

data class DataConfig(override val isVadEnabled: Boolean = true,
                      override val url: URL,
                      override val refreshToken: String,
                      override val userId: String?,
                      override val audioRecordParams: AudioRecordParams? = null) : Config