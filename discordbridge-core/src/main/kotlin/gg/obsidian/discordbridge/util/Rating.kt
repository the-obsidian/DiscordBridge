package gg.obsidian.discordbridge.util

//TODO: Make null safe
/**
 * Represents a Rating entry in rate.yml
 *
 * @param message the rating message template
 * @param low the lower bound of ratings that will trigger this message
 * @param high the upper bound of ratings that will trigger this message
 */
data class Rating(val message: String = "%u - I rate %m %r") {

    var low: Double
    var high: Double

    init {
        low = 0.0
        high = 0.0
    }

    constructor(map: MutableMap<String, Any>):
            this(map["message"] as String) {
        low = if(map["low"] is Double) map["low"] as Double else (map["low"] as Int).toDouble()
        high = if(map["high"] is Double) map["high"] as Double else (map["high"] as Int).toDouble()
    }

    constructor(message: String, low: Double = 0.0, high: Double = 0.0): this(message)
}