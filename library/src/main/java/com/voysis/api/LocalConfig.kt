package com.voysis.api

interface LocalConfig : BaseConfig {

    /**
     * @return resourcePath is a reference to the necessary binary sets for local execution
     */
    val resourcePath: String
}
