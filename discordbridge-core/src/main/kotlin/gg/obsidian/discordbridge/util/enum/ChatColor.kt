package gg.obsidian.discordbridge.util.enum

import org.apache.commons.lang.Validate
import java.util.regex.Pattern

private const val COLOR_CHAR = '\u00A7'

/**
 * All supported color values for chat
 */
enum class ChatColor(
        val char: Char, private val intCode: Int,
        private val isFormat: Boolean = false) {
    BLACK('0', 0x00),
    DARK_BLUE('1', 0x1),
    DARK_GREEN('2', 0x2),
    DARK_AQUA('3', 0x3),
    DARK_RED('4', 0x4),
    DARK_PURPLE('5', 0x5),
    GOLD('6', 0x6),
    GRAY('7', 0x7),
    DARK_GRAY('8', 0x8),
    BLUE('9', 0x9),
    GREEN('a', 0xA),
    AQUA('b', 0xB),
    RED('c', 0xC),
    LIGHT_PURPLE('d', 0xD),
    YELLOW('e', 0xE),
    WHITE('f', 0xF),
    MAGIC('k', 0x10, true),
    BOLD('l', 0x11, true),
    STRIKETHROUGH('m', 0x12, true),
    UNDERLINE('n', 0x13, true),
    ITALIC('o', 0x14, true),
    RESET('r', 0x15);

    private val toString: String = String(charArrayOf(COLOR_CHAR, char))

    val isColor: Boolean
        get() = !isFormat && this != RESET

    override fun toString(): String {
        return toString
    }

    companion object {

        /**
         * The special character which prefixes all chat colour codes. Use this if
         * you need to dynamically convert colour codes from your custom format.
         */
        private val STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + getColorChar().toString() + "[0-9A-FK-OR]")
        private val BY_ID = HashMap<Int, ChatColor>()
        private val BY_CHAR = HashMap<Char, ChatColor>()

        fun getColorChar(): Char = '\u00A7'

        /**
         * Gets the color represented by the specified color code
         *
         * @param code Code to check
         * @return Associative [org.bukkit.ChatColor] with the given code,
         * or null if it doesn't exist
         */
        fun getByChar(code: Char): ChatColor? {
            return BY_CHAR[code]
        }

        /**
         * Gets the color represented by the specified color code
         *
         * @param code Code to check
         * @return Associative [org.bukkit.ChatColor] with the given code,
         * or null if it doesn't exist
         */
        fun getByChar(code: String): ChatColor {
            Validate.notNull(code, "Code cannot be null")
            Validate.isTrue(code.length > 0, "Code must have at least one char")

            return BY_CHAR[code[0]]!!
        }

        /**
         * Strips the given message of all color codes
         *
         * @param input String to strip of color
         * @return A copy of the input string, without any coloring
         */
        fun stripColor(input: String): String {
            return STRIP_COLOR_PATTERN.matcher(input).replaceAll("")
        }

        /**
         * Translates a string using an alternate color code character into a
         * string that uses the internal ChatColor.COLOR_CODE color code
         * character. The alternate color code character will only be replaced if
         * it is immediately followed by 0-9, A-F, a-f, K-O, k-o, R or r.
         *
         * @param altColorChar The alternate color code character to replace. Ex: &
         * @param textToTranslate Text containing the alternate color code character.
         * @return Text containing the ChatColor.COLOR_CODE color code character.
         */
        fun translateAlternateColorCodes(altColorChar: Char, textToTranslate: String): String {
            val b = textToTranslate.toCharArray()
            for (i in 0 until b.size - 1) {
                if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
                    b[i] = getColorChar()
                    b[i + 1] = Character.toLowerCase(b[i + 1])
                }
            }
            return String(b)
        }

        /**
         * Gets the ChatColors used at the end of the given input string.
         *
         * @param input Input string to retrieve the colors from.
         * @return Any remaining ChatColors to pass onto the next line.
         */
        fun getLastColors(input: String): String {
            var result = ""
            val length = input.length

            // Search backwards from the end as it is faster
            for (index in length - 1 downTo -1 + 1) {
                val section = input[index]
                if (section == COLOR_CHAR && index < length - 1) {
                    val c = input[index + 1]
                    val color = getByChar(c)

                    if (color != null) {
                        result = color.toString() + result

                        // Once we find a color or reset we can stop searching
                        if (color.isColor || color == RESET) {
                            break
                        }
                    }
                }
            }

            return result
        }

        init {
            for (color in values()) {
                BY_ID.put(color.intCode, color)
                BY_CHAR.put(color.char, color)
            }
        }
    }
}
