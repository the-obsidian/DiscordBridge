package gg.obsidian.discordbridge

/*
* Copyright (C) 2012 SagaciousZed
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.logging.Level

class ConfigAccessor(private val plugin: JavaPlugin, private val fileName: String) {

    private val configFile: File?
    private var fileConfiguration: FileConfiguration? = null

    init {
        plugin.dataFolder ?: throw IllegalStateException()
        this.configFile = File(plugin.dataFolder, fileName)
    }

    fun reloadConfig() {
        fileConfiguration = YamlConfiguration.loadConfiguration(configFile!!)

        // Look for defaults in the jar
        val defConfigReader = InputStreamReader(plugin.getResource(fileName))
        val defConfig = YamlConfiguration.loadConfiguration(defConfigReader)
        fileConfiguration!!.defaults = defConfig
    }

    val config: FileConfiguration
        get() {
            if (fileConfiguration == null) {
                this.reloadConfig()
            }
            return fileConfiguration!!
        }

    fun saveConfig() {
        if (fileConfiguration == null || configFile == null) {
            return
        } else {
            try {
                config.save(configFile)
            } catch (ex: IOException) {
                plugin.logger.log(Level.SEVERE, "Could not save config to " + configFile, ex)
            }

        }
    }

    @Suppress("unused")
    fun saveDefaultConfig() {
        if (!configFile!!.exists()) {
            this.plugin.saveResource(fileName, false)
        }
    }

}
