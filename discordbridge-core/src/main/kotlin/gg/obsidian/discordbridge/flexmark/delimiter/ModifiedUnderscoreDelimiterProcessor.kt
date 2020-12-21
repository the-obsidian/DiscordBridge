package gg.obsidian.discordbridge.flexmark.delimiter

import com.vladsch.flexmark.ast.Emphasis
import com.vladsch.flexmark.parser.core.delimiter.Delimiter
import com.vladsch.flexmark.parser.core.delimiter.UnderscoreDelimiterProcessor
import com.vladsch.flexmark.util.ast.DelimitedNode
import com.vladsch.flexmark.util.sequence.BasedSequence

class ModifiedUnderscoreDelimiterProcessor : UnderscoreDelimiterProcessor(true) {
    override fun process(opener: Delimiter, closer: Delimiter, delimitersUsed: Int) {
        val emphasis: DelimitedNode = if (delimitersUsed == 1) Emphasis(
            opener.getTailChars(delimitersUsed),
            BasedSequence.NULL,
            closer.getLeadChars(delimitersUsed)
        ) else Underline(
            opener.getTailChars(delimitersUsed),
            BasedSequence.NULL,
            closer.getLeadChars(delimitersUsed)
        )
        opener.moveNodesBetweenDelimitersTo(emphasis, closer)
    }
}
