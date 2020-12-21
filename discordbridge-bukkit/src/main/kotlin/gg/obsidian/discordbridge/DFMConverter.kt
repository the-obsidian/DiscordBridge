package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.flexmark.DFMParser
import com.vladsch.flexmark.ast.Text as TextNode
import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough
import gg.obsidian.discordbridge.flexmark.NodeVisitor
import gg.obsidian.discordbridge.flexmark.delimiter.Spoiler
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.hover.content.Text as HoverText

class DFMConverter(private val builder: ComponentBuilder) : NodeVisitor() {
    override fun processSpoilerNode(node: Spoiler) {
        builder.append(node.baseSequence.toString())
            .obfuscated(true)
            .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, HoverText(node.chars.toString())))
    }

    override fun processStrikethroughNode(node: Strikethrough) {
        builder.append(node.baseSequence.toString())
            .strikethrough(true)
    }

    override fun processEmphasisNode(node: Emphasis) {
        builder.append(node.baseSequence.toString())
            .italic(true)
    }

    override fun processStrongEmphasisNode(node: StrongEmphasis) {
        builder.append(node.baseSequence.toString())
            .bold(true)
    }

    override fun processCodeNode(node: Code) {
        builder.append(node.baseSequence.toString())
    }

    override fun processTextNode(node: TextNode) {
        builder.append(node.baseSequence.toString())
    }

    fun finalize(): Array<out BaseComponent> {
        return builder.create()
    }

    companion object {
        fun convert(text: String): Array<out BaseComponent> {
            val ast = DFMParser().parse(text)
            return DFMConverter(ComponentBuilder()).run {
                visit(ast)
                finalize()
            }
        }
    }
}
