package com.voysis.wakeword

data class WakewordConfig(
        //represents the sample size of the sliding window scale.
        val sampleWindowSize: Int = 800,
        //input size for wakeword model.
        val sampleSize: Int = 24000,
        //the amount of positive interpreter responses must be reached before wakeword detection is returned
        val detectionThreshold: Int = 7)