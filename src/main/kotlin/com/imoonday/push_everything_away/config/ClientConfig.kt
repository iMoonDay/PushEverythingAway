package com.imoonday.push_everything_away.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.imoonday.push_everything_away.PushEverythingAway
import net.fabricmc.loader.api.FabricLoader
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class ClientConfig {

    var chargeProgressBarSettings = ProgressBarSettings()
        set(chargeProgressBarSettings) {
            field = chargeProgressBarSettings
            save()
        }

    var displaySelectionOutlines = true
        set(displaySelectionOutlines) {
            field = displaySelectionOutlines
            save()
        }

    fun toJson(): String = GSON.toJson(this)

    companion object {
        private const val ENVIRONMENT = "client"
        private val GSON = GsonBuilder().setPrettyPrinting().create()
        private const val MOD_ID = PushEverythingAway.MOD_ID
        private var file: File? = null
            get() {
                if (field == null) {
                    field =
                        FabricLoader.getInstance().configDir.resolve("$MOD_ID-$ENVIRONMENT.json").toFile()
                }
                return field
            }
        var instance = ClientConfig()

        fun load() {
            println("Loading $MOD_ID $ENVIRONMENT configuration file\n")
            try {
                val file = file
                if (!file!!.exists()) {
                    save()
                }
                if (file.exists()) {
                    val br = BufferedReader(FileReader(file))
                    val jsonElement = JsonParser.parseReader(br)
                    val config = fromJson(jsonElement.toString())
                    setInstance(config, true)
                }
            } catch (e: Exception) {
                save()
            }
        }

        fun save() {
            val json: String = try {
                instance.toJson()
            } catch (e: Exception) {
                e.localizedMessage
            }
            val file = file!!
            try {
                FileWriter(file).use { fileWriter -> fileWriter.write(json) }
            } catch (e: Exception) {
                println("Couldn't save $MOD_ID $ENVIRONMENT.name configuration file\n")
                e.printStackTrace()
            }
        }

        private fun setInstance(config: ClientConfig?, saveIfFailed: Boolean) {
            if (config != null) {
                instance = config
            } else if (saveIfFailed) {
                println(
                    "Read $MOD_ID $ENVIRONMENT.name configuration failed. Try to save the current configuration\n"
                )
                save()
            }
        }

        private fun fromJson(json: String?): ClientConfig? = GSON.fromJson(json, ClientConfig::class.java)
    }
}
