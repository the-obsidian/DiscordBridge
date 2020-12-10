package gg.obsidian.discordbridge.util

import com.vladsch.flexmark.util.ast.NodeVisitor
import com.vladsch.flexmark.util.ast.VisitHandler
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough
import com.vladsch.flexmark.formatter.Formatter
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.ParserEmulationProfile
import com.vladsch.flexmark.ast.Emphasis
import com.vladsch.flexmark.ast.StrongEmphasis
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.sequence.BasedSequence

/**
 * This object converts strings formatted with basic Discord-flavored Markdown syntax into
 * equivalent strings formatted with Minecraft formatting symbols
 */
object MarkdownToMinecraftSeralizer {
    private val OPTIONS = MutableDataSet()

    init {
        OPTIONS.setFrom(ParserEmulationProfile.COMMONMARK)
    }

    private val PARSER = Parser.builder(OPTIONS).build()
    private val VISITOR = NodeVisitor(
        VisitHandler(Strikethrough::class.java, this::visit),
        VisitHandler(Emphasis::class.java, this::visit),
        VisitHandler(StrongEmphasis::class.java, this::visit)
    )
    private val RENDERER = Formatter.builder(OPTIONS).build()

    private fun visit(node: Strikethrough) {
        node.openingMarker = BasedSequence.of("\u00A7m")
        node.closingMarker = BasedSequence.of("\u00A7r")
    }

    private fun visit(node: Emphasis) {
        node.openingMarker = BasedSequence.of("\u00A7l")
        node.closingMarker = BasedSequence.of("\u00A7r")
    }

    private fun visit(node: StrongEmphasis) {
        node.openingMarker = BasedSequence.of("\u00A7n")
        node.closingMarker = BasedSequence.of("\u00A7r")
    }

    /**
     * Descends a parse tree and returns a formatted string
     *
     * @param input = input string
     * @return a string formatted with Minecraft formatting symbols
     */
    fun toMinecraft(input: String): String {
        val document = PARSER.parse(input)
        VISITOR.visit(document)
        return RENDERER.render(document)
    }
}
