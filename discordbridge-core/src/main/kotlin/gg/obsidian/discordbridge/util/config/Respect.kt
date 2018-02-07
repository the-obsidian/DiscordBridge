package gg.obsidian.discordbridge.util.config

//TODO: make null safe
/**
 * Represents a Respect entry in f.yml
 *
 * @param message the respects message template
 * @param count the number of respects that will be paid
 * @param weight the probability weight that this entry will be selected
 */
data class Respect(val message: String = "%u solemnly pays respect!", val count: Int = 1, val weight: Int = 1) {
    constructor(map: MutableMap<String, Any>):
            this(map["message"] as String, map["respects-paid"] as Int, map["weight"] as Int)
}
