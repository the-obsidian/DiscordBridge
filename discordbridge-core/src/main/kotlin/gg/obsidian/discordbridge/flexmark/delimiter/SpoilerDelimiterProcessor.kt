package gg.obsidian.discordbridge.flexmark.delimiter

import com.vladsch.flexmark.parser.InlineParser
import com.vladsch.flexmark.parser.core.delimiter.Delimiter
import com.vladsch.flexmark.parser.delimiter.DelimiterProcessor
import com.vladsch.flexmark.parser.delimiter.DelimiterRun
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.sequence.BasedSequence

object SpoilerDelimiterProcessor : DelimiterProcessor {
    private const val openingCharacter = '|'
    private const val closingCharacter = '|'
    private const val minLength = 2

    override fun canBeOpener(
        before: String?,
        after: String?,
        leftFlanking: Boolean,
        rightFlanking: Boolean,
        beforeIsPunctuation: Boolean,
        afterIsPunctuation: Boolean,
        beforeIsWhitespace: Boolean,
        afterIsWhiteSpace: Boolean
    ): Boolean {
        return leftFlanking
    }

    override fun canBeCloser(
        before: String?,
        after: String?,
        leftFlanking: Boolean,
        rightFlanking: Boolean,
        beforeIsPunctuation: Boolean,
        afterIsPunctuation: Boolean,
        beforeIsWhitespace: Boolean,
        afterIsWhiteSpace: Boolean
    ): Boolean {
        return rightFlanking
    }

    override fun skipNonOpenerCloser(): Boolean {
        return false
    }

    override fun getDelimiterUse(opener: DelimiterRun, closer: DelimiterRun): Int {
        return if (opener.length() >= 2 && closer.length() >= 2) 2 else 0
    }

    override fun unmatchedDelimiterNode(inlineParser: InlineParser, delimiter: DelimiterRun): Node? {
        return null
    }

    override fun getOpeningCharacter(): Char = openingCharacter
    override fun getClosingCharacter(): Char = closingCharacter
    override fun getMinLength(): Int = minLength

    override fun process(opener: Delimiter, closer: Delimiter, delimitersUsed: Int) {
        val spoiler =
            Spoiler(opener.getTailChars(delimitersUsed), BasedSequence.NULL, closer.getLeadChars(delimitersUsed))
        opener.moveNodesBetweenDelimitersTo(spoiler, closer)
    }
}
