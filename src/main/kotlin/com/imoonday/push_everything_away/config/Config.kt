package com.imoonday.push_everything_away.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.imoonday.push_everything_away.PushEverythingAway
import com.imoonday.push_everything_away.utils.HammerMaterial
import net.fabricmc.loader.api.FabricLoader
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class Config {
    var enchantmentSettings = EnchantmentSettings()
        set(enchantmentSettings) {
            field = enchantmentSettings
            save()
        }

    var groundHammerAttributesOverride: Map<String, Map<String, Number>> =
        HammerMaterial.entries.associate {
            it.toString() to linkedMapOf(
                "baseDistance" to it.baseDistance,
                "maxPushStrength" to it.maxPushStrength,
                "maxRange" to it.maxRange,
                "pushStrengthIntervalMultiplier" to it.pushStrengthIntervalMultiplier,
                "pushRangeIntervalMultiplier" to it.pushRangeIntervalMultiplier
            )
        }
        set(value) {
            field = value
            save()
        }

    fun toJson(): String = GSON.toJson(this)

    companion object {
        private const val ENVIRONMENT = "server"
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
        var instance = Config()

        fun load() {
            println("Loading $MOD_ID $ENVIRONMENT configuration file")
            try {
                val file = file!!
                if (!file.exists()) {
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
                FileWriter(file).use { it.write(json) }
            } catch (e: Exception) {
                println("Couldn't save $MOD_ID $ENVIRONMENT configuration file")
                e.printStackTrace()
            }
        }

        fun setInstance(config: Config?, saveIfFailed: Boolean) {
            if (config != null) {
                instance = config
            } else if (saveIfFailed) {
                println(
                    "Read $MOD_ID $ENVIRONMENT.name configuration failed. Try to save the current configuration"
                )
                save()
            }
        }

        fun fromJson(json: String?): Config? = GSON.fromJson(json, Config::class.java)
    }
}
