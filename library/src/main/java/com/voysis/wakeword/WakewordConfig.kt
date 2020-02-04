package com.voysis.wakeword

data class WakewordConfig(
        //represents the sample size of the sliding window scale.
        val sampleWindowSize: Int = 800,
        //input size for wakeword model.
        val sampleSize: Int = 24000,
        //interpreter output needs to be above this threshold in order for activation to be recognised
        val probThreshold: Float = 0.55f,
        //amount of activations that need to be registered before detection registered
        val thresholdCount: Int = 1)
