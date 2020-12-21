package gg.obsidian.discordbridge.flexmark

import com.vladsch.flexmark.ast.Code
import com.vladsch.flexmark.ast.Paragraph
import com.vladsch.flexmark.ast.Text
import com.vladsch.flexmark.ast.util.Parsing
import com.vladsch.flexmark.ext.gfm.strikethrough.internal.StrikethroughDelimiterProcessor
import com.vladsch.flexmark.parser.core.delimiter.AsteriskDelimiterProcessor
import com.vladsch.flexmark.parser.core.delimiter.Delimiter
import com.vladsch.flexmark.parser.core.delimiter.UnderscoreDelimiterProcessor
import com.vladsch.flexmark.parser.delimiter.DelimiterProcessor
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.sequence.BasedSequence
import com.vladsch.flexmark.util.sequence.SequenceUtils
import gg.obsidian.discordbridge.flexmark.delimiter.SpoilerDelimiterProcessor
import java.util.BitSet
import java.util.regex.Pattern
import kotlin.collections.HashMap

/**
 * Parser for Discord-flavored Markdown (DFM).
 */
class DFMParser {
    private val myParsing = Parsing(null)
    private val specialChars = BitSet()
    private val delimiterChars = BitSet()
    private val delimiterProcessors = mapOf(
        Pair('|', SpoilerDelimiterProcessor),
        Pair('*', AsteriskDelimiterProcessor(true)),
        Pair('_', UnderscoreDelimiterProcessor(true)),
        Pair('~', StrikethroughDelimiterProcessor())
    )

    private lateinit var document: Paragraph
    private lateinit var input: BasedSequence
    private var index = 0
    private var delimiterStackTop: Delimiter? = null
    private val plainTextBuffer: MutableList<BasedSequence> = mutableListOf()

    init {
        listOf('\\', '`', '|', '*', '_', '~').map { specialChars.set(it.toInt()) }
        listOf('|', '*', '_', '~').map { delimiterChars.set(it.toInt()) }
    }

    /**
     * Parses an input String into an abstract syntax tree (AST).
     */
    fun parse(input: String) : Paragraph {
        this.input = BasedSequence.of(input.trim())
        document = Paragraph()
        index = 0
        delimiterStackTop = null

        while (parseChar()) { /* noop */ }
        processDelimiters()
        flushPlainText()
        mergeAllTextNodes()

        return document
    }

    /**
     * Appends the given ChildNode to the Document.
     *
     * plainTextBuffer is automatically flushed before appending.
     */
    private fun appendNode(node: Node) {
        flushPlainText()
        document.appendChild(node)
    }

    /**
     * Appends a new Text node with the contents of the given
     * BasedSequence to the Document.
     *
     * plainTextBuffer is automatically flushed before appending.
     */
    private fun appendSeparateText(text: BasedSequence): Text {
        val node = Text(text)
        appendNode(node)
        return node
    }

    /**
     * Appends the contents of plainTextBuffer and appends it to
     * the Document as a new Text node.
     *
     * This clears plainTextBuffer.
     **/
    private fun flushPlainText() {
        if (plainTextBuffer.size > 0) {
            val first = plainTextBuffer.removeFirst()
            val node = Text(first.builder.addAll(plainTextBuffer).toSequence())
            document.appendChild(node)
            plainTextBuffer.clear()
        }
    }

    /**
     * Steps through the Document and collapses all chains of
     * sequential Text nodes into single Text nodes.
     */
    private fun mergeAllTextNodes() {
        var first: Text? = null
        var last: Text? = null

        var node = document.firstChild

        while (node != null) {
            if (node is Text) {
                val text = node
                if (first == null) first = text
                last = text
            } else {
                mergeTextNodes(first, last)
                first = null
                last = null
            }

            if (node === document.lastChild) {
                break
            }

            node = node.next
        }
        mergeTextNodes(first, last)
    }

    /**
     * Attempts to merge a chain of sequential Text nodes into a single Text node.
     * @param first Text node at the beginning of the sequence
     * @param last Text node at the end of the sequence
     */
    private fun mergeTextNodes(first: Text?, last: Text?) {
        if (first == null || last == null || first == last) return

        val sb = mutableListOf(first.chars)

        var node = first.next
        while (node !== last.next) {
            sb.add(node!!.chars)
            val unlink = node
            node = node.next
            unlink.unlink()
        }

        val builder = StringBuilder()
        sb.map { builder.append(it) }
        first.chars = first.chars.builder.addAll(sb).toSequence()
    }

    /**
     * Parses a single character from the input sequence.
     * @return False if EOL is reached, otherwise true.
     */
    private fun parseChar(): Boolean {
        val c = peek()
        if (c == SequenceUtils.NUL) return false

        val processSuccess = when (c) {
            '\\' -> parseBackslash()
            '`' -> parseBackticks()
            else -> {
                if (delimiterChars.get(c.toInt())) {
                    val useProcessor = delimiterProcessors[c] ?: return false
                    parseDelimiters(useProcessor, c)
                } else {
                    parseString()
                    true
                }
            }
        }

        if (!processSuccess) {
            index++
            plainTextBuffer.add((input.subSequence(index - 1, index)))
        }

        return true
    }

    /**
     * Scans the portion of the input sequence starting at the
     * current index position for the first subsequence that
     * matches the given pattern.
     *
     * If a match is found, all characters leading up to and
     * including the match are consumed from the input sequence
     * and the matched subsequence is returned.
     *
     * Otherwise, no characters are consumed and this returns null.
     */
    private fun match(re: Pattern): BasedSequence? {
        if (index >= input.length) return null

        val matcher = re.matcher(input)
        matcher.region(index, input.length)
        val m = matcher.find()

        return if (m) {
            index = matcher.end()
            val result = matcher.toMatchResult()
            input.subSequence(result.start(), result.end())
        } else {
            null
        }
    }

    /**
     * Gets the value of the character in the input
     * sequence at the current index without consuming it.
     * @param offset Offets the retrieval index by the given amount.
     */
    private fun peek(offset:Int = 0): Char {
        return if (0 <= index + offset && index + offset < input.length) input[index + offset]
        else SequenceUtils.NUL
    }

    /**
     * Processes a backslash character from the input sequence.
     *
     * Consumes the backslash character from the input sequence.
     * If the character immediately following the backslash is an
     * escapable character, it is also consumed. The consumed
     * characters are added to plainTextBuffer.
     */
    private fun parseBackslash(): Boolean {
        val offset = if (myParsing.ESCAPABLE.matcher(peek(1).toString()).matches()) 2 else 1
        plainTextBuffer.add(input.subSequence(index, index + offset))
        index += offset
        return true
    }

    /**
     * Process a string of backtick characters from the input sequence.
     *
     * Collects as many adjacent backtick characters from the input sequence
     * as possible, then checks the remainder of the sequence for a matching
     * set of backticks. If a pair is found, the backtick strings and all
     * characters between them are consumed from the input sequence and
     * added to the AST as a new Code node.
     */
    private fun parseBackticks(): Boolean {
        val ticks = match(myParsing.TICKS_HERE) ?: return false
        val afterOpenTicks = index

        var matched: BasedSequence
        while (match(myParsing.TICKS).also { matched = it!! } != null) {
            if (matched == ticks) {
                val ticksLength = ticks.length
                val codeText = input.subSequence(afterOpenTicks, index - ticksLength)
                val node = Code(
                    input.subSequence(afterOpenTicks - ticksLength, afterOpenTicks),
                    codeText,
                    input.subSequence(index - ticksLength, index)
                )

                appendNode(node)
                return true
            }
        }

        // If we got here, we didn't match a closing backtick sequence.
        index = afterOpenTicks
        plainTextBuffer.add(ticks)
        return true
    }

    /**
     * A simple struct representing the result of a call to parseDelimiters().
     */
    private class DelimiterData constructor(val count: Int, val canOpen: Boolean, val canClose: Boolean)

    /**
     * Attempts to parse a new Delimiter from the input sequence
     * using the given processor.
     *
     * If successful, plainTextBuffer is flushed, the characters
     * associated with the Delimiter are appended to the Document
     * as a new Text node, and a Delimiter pointing to that node is
     * pushed to the Delimiter stack.
     *
     * Otherwise, this does nothing.
     */
    private fun parseDelimiters(delimiterProcessor: DelimiterProcessor, delimiterChar: Char): Boolean {
        val res = scanDelimiters(delimiterProcessor, delimiterChar) ?: return false
        val node = appendSeparateText(input.subSequence(index, index + res.count))

        val delimiter = Delimiter(
            input,
            node,
            delimiterChar,
            res.canOpen,
            res.canClose,
            delimiterStackTop,
            index
        )
        delimiter.numDelims = res.count

        val prev = delimiter.previous
        if (prev != null) prev.next = delimiter
        delimiterStackTop = delimiter
        index += res.count
        return true
    }

    /**
     * Consumes characters from the input sequence until a
     * special character is reached, and appends the consumed
     * characters to plainTextBuffer.
     */
    private fun parseString() {
        val begin = index
        index++

        while (index != input.length) {
            val c = input[index]
            if (specialChars[c.toInt()]) break
            index++
        }

        plainTextBuffer.add(input.subSequence(begin, index))
    }

    /**
     * Looks ahead at the sequence to verify that the delimiter
     * being parsed is adequate to open or close the node type
     * corresponding to the given processor.
     */
    private fun scanDelimiters(delimiterProcessor: DelimiterProcessor, delimiterChar: Char): DelimiterData? {
        var delimiterCount = 0
        while (peek(delimiterCount) == delimiterChar) delimiterCount++
        if (delimiterCount < delimiterProcessor.minLength) return null

        val before = if (peek(-1) == SequenceUtils.NUL) SequenceUtils.EOL else peek(-1).toString()
        val after = if (peek(delimiterCount) == SequenceUtils.NUL) SequenceUtils.EOL else peek(delimiterCount).toString()
        val beforeIsWhitespace = myParsing.UNICODE_WHITESPACE_CHAR.matcher(before).matches()
        val afterIsWhitespace = myParsing.UNICODE_WHITESPACE_CHAR.matcher(after).matches()
        val beforeIsPunctuation = myParsing.PUNCTUATION.matcher(before).matches()
        val afterIsPunctuation = myParsing.PUNCTUATION.matcher(after).matches()
        val leftFlanking = !afterIsWhitespace && !(afterIsPunctuation && !beforeIsWhitespace && !beforeIsPunctuation)
        val rightFlanking = !beforeIsWhitespace && !(beforeIsPunctuation && !afterIsWhitespace && !afterIsPunctuation)

        val canOpen = delimiterChar == delimiterProcessor.openingCharacter && delimiterProcessor.canBeOpener(
            before,
            after,
            leftFlanking,
            rightFlanking,
            beforeIsPunctuation,
            afterIsPunctuation,
            beforeIsWhitespace,
            afterIsWhitespace
        )

        val canClose = delimiterChar == delimiterProcessor.closingCharacter && delimiterProcessor.canBeCloser(
            before,
            after,
            leftFlanking,
            rightFlanking,
            beforeIsPunctuation,
            afterIsPunctuation,
            beforeIsWhitespace,
            afterIsWhitespace
        )

        return when {
            (!delimiterProcessor.skipNonOpenerCloser()) -> DelimiterData(delimiterCount, canOpen, canClose)
            (canOpen || canClose) -> DelimiterData(delimiterCount, canOpen, canClose)
            else -> null
        }
    }

    /**
     * Iterates through the Delimiter stack, looking for pairs of
     * Delimiters that can be converted into DelimitedNodes and
     * inserted into the AST.
     *
     * All Delimiters that do not become DelimitedNodes are
     * truncated from the stack, and their corresponding Text nodes
     * are merged with adjacent Text nodes where possible.
     */
    private fun processDelimiters() {
        if (delimiterStackTop == null) return

        val openerLowerBounds: MutableMap<Char, Delimiter> = HashMap()

        // Descend to the beginning of the stack
        var closer = delimiterStackTop
        while (closer!!.previous !== null) closer = closer!!.previous

        // Ascend back up the stack, looking for closers and attempting to handle them
        while (closer != null) {
            if (!closer.canClose()) {
                closer = closer.next
                continue
            }

            // Found a Delimiter that can act as a closer
            val delimiterChar = closer.delimiterChar
            val delimiterProcessor = delimiterProcessors[delimiterChar] ?: continue
            var delimitersUsed = 0
            var isOpenerFound = false
            var isOpenerTypeFound = false

            // Look back into the stack for a matching opener
            var opener = closer.previous
            while (opener != null) {
                if (opener === openerLowerBounds[delimiterChar]) {
                    // This Delimiter is a known dead-end. Abandon search.
                    break
                }

                if (opener.canOpen() && opener.delimiterChar == delimiterProcessor.openingCharacter) {
                    isOpenerTypeFound = true
                    delimitersUsed = delimiterProcessor.getDelimiterUse(opener, closer)
                    if (delimitersUsed > 0) {
                        isOpenerFound = true
                        break
                    }
                }

                opener = opener.previous
            }
            if (opener == null) {

                continue
            }

            if (!isOpenerFound) {
                if (!isOpenerTypeFound) {
                    // No openers of the same type were found.
                    // We can mark the Delimiter before this closer as a dead-end
                    // to future searches for corresponding openers of this type.
                    openerLowerBounds[delimiterChar] = closer.previous!!

                    if (!closer.canOpen()) {
                        // If this closer has no possible matching opener, and cannot
                        // act as an opener for another closer, it is a useless.
                        removeDelimiterKeepNode(closer)
                    }
                }
                closer = closer.next
                continue
            }

            // Removes the Text nodes associated with the opener and closer
            // from the AST and replaces them with a new DelimitedNode.
            // All sibling nodes between the removed nodes, if any, are made
            // children of this new DelimitedNode.
            delimiterProcessor.process(opener, closer, delimitersUsed)

            // By definition, we've found the most inner-nested Delimiter pair
            // possible from the start of the input sequence. If any delimiters
            // remain between the opener and closer, they must therefore be
            // useless and can safely be dropped from the Delimiter stack.
            removeDelimitersBetween(opener, closer)

            // Consume characters from the delimiters of the opener and closer.
            // If not all characters are consumed, they may be used again for
            // future matches. Otherwise, they and their corresponding Text
            // nodes can be dropped completely, as they will be empty.
            opener.numDelims -= delimitersUsed
            if (opener.numDelims == 0) {
                removeDelimiterAndNode(opener)
            } else {
                opener.node.chars = opener.node.chars.subSequence(0, opener.numDelims)
            }

            closer.numDelims -= delimitersUsed
            if (closer.numDelims == 0) {
                val next = closer.next
                removeDelimiterAndNode(closer)
                closer = next
            } else {
                val chars = closer.node.chars
                val length = chars.length
                closer.node.chars = chars.subSequence(length - closer.numDelims, length)
                closer.index = closer.index + delimitersUsed
            }
        }

        // All pairs found!
        // Flush the Delimiter stack if it is not empty.
        while (delimiterStackTop != null) {
            removeDelimiterKeepNode(delimiterStackTop!!)
        }
    }

    /**
     * Drops all Delimiters from the Delimiter stack that exist
     * between the two inputs (non-inclusive).
     */
    private fun removeDelimitersBetween(opener: Delimiter, closer: Delimiter) {
        var delimiter = closer.previous
        while (delimiter != null && delimiter !== opener) {
            val previousDelimiter = delimiter.previous
            removeDelimiterKeepNode(delimiter)
            delimiter = previousDelimiter
        }
    }

    /**
     * Drops the given Delimiter from the Delimiter stack,
     * and also removes its corresponding Text node from the AST.
     *
     * If this Text node was between two other Text nodes, those
     * Text nodes will be merged together.
     */
    private fun removeDelimiterAndNode(delim: Delimiter) {
        val node = delim.node
        val previousText = delim.previousNonDelimiterTextNode
        val nextText = delim.nextNonDelimiterTextNode

        if (previousText != null && nextText != null) {
            previousText.chars = input.baseSubSequence(previousText.startOffset, nextText.endOffset)
            nextText.unlink()
        }

        node.unlink()
        removeDelimiter(delim)
    }

    /**
     * Drops the given Delimiter from the Delimiter stack,
     * but keeps its corresponding Text node in the AST.
     *
     * If this Text node has other Text nodes immediately adjacent,
     * those Text nodes will be merged into this one.
     */
    private fun removeDelimiterKeepNode(delim: Delimiter) {
        val node = delim.node
        val previousText = delim.previousNonDelimiterTextNode
        val nextText = delim.nextNonDelimiterTextNode

        when {
            nextText != null && previousText != null -> {
                node.chars = input.baseSubSequence(previousText.startOffset, nextText.endOffset)
                previousText.unlink()
                nextText.unlink()
            }
            previousText != null -> {
                node.chars = input.baseSubSequence(previousText.startOffset, node.endOffset)
                previousText.unlink()
            }
            nextText != null -> {
                node.chars = input.baseSubSequence(node.startOffset, nextText.endOffset)
                nextText.unlink()
            }
        }

        removeDelimiter(delim)
    }

    /**
     * Drops a given Delimiter from the Delimiter stack.
     */
    private fun removeDelimiter(delim: Delimiter) {
        val prev = delim.previous
        val next = delim.next

        if (prev != null) prev.next = next

        if (next == null) delimiterStackTop = prev
        else next.previous = prev
    }
}
