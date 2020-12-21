package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.flexmark.DFMParser
import com.vladsch.flexmark.ast.Text as TextNode
import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough
import gg.obsidian.discordbridge.flexmark.NodeVisitor
import gg.obsidian.discordbridge.flexmark.delimiter.Spoiler
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.ComponentBuilder.FormatRetention
import net.md_5.bungee.api.chat.HoverEvent
import java.util.*
import net.md_5.bungee.api.chat.hover.content.Text as HoverText

class DFMConverter(private val builder: ComponentBuilder) : NodeVisitor() {
    private val context = Stack<Context>()

    init {
        context.push(Context())
    }

    override fun processSpoilerNode(node: Spoiler) {
        val spoilerText = DFMConverter(ComponentBuilder()).run {
            visit(node)
            finalize()
        }

        builder.append(node.text.toString())
            .obfuscated(true)
            .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, HoverText(spoilerText)))
    }

    override fun processStrikethroughNode(node: Strikethrough) {
        context.push(Context.createFrom(context.peek(), isStrike = true))
        processNodeChildren(node)
        context.pop()
    }

    override fun processEmphasisNode(node: Emphasis) {
        context.push(Context.createFrom(context.peek(), isItalic = true))
        processNodeChildren(node)
        context.pop()
    }

    override fun processStrongEmphasisNode(node: StrongEmphasis) {
        context.push(Context.createFrom(context.peek(), isBold = true))
        processNodeChildren(node)
        context.pop()
    }

    override fun processCodeNode(node: Code) {
        //TODO
        processNodeChildren(node)
    }

    override fun processTextNode(node: TextNode) {
        val ctx = context.peek()
        builder.append(node.chars.toString(), FormatRetention.NONE)
            .bold(ctx.isBold)
            .italic(ctx.isItalic)
            .underlined(ctx.isUnderline)
            .strikethrough(ctx.isStrike)
            .obfuscated(ctx.isObfuscated)
        processNodeChildren(node)
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

    private class Context(
        val isItalic: Boolean = false,
        val isBold: Boolean = false,
        val isStrike: Boolean = false,
        val isUnderline: Boolean = false,
        val isObfuscated: Boolean = false
    ) {
        companion object {
            fun createFrom(
                other: Context,
                isItalic: Boolean = false,
                isBold: Boolean = false,
                isStrike: Boolean = false,
                isUnderline: Boolean = false,
                isObfuscated: Boolean = false
            ): Context {
                return Context(
                    isItalic || other.isItalic,
                    isBold || other.isBold,
                    isStrike || other.isStrike,
                    isUnderline || other.isUnderline,
                    isObfuscated || other.isObfuscated
                )
            }
        }
    }
}
