package io.pleo.antaeus.app

import java.util.*

class ConfigLoader(private val configFilename : String) {

    val config = Properties()

    init {
        config.load(ConfigLoader::class.java.classLoader.getResourceAsStream(configFilename))
    }

    fun get(propertyName : String?): String {
        return config
            .getProperty(propertyName)
            .orEmpty()
    }
}
