package gg.obsidian.discordbridge.flexmark

import com.vladsch.flexmark.ast.*
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough
import com.vladsch.flexmark.util.ast.Node
import gg.obsidian.discordbridge.flexmark.delimiter.Spoiler
import gg.obsidian.discordbridge.flexmark.delimiter.Underline

abstract class NodeVisitor {
    fun visit(node: Node) {
        processNodeChildren(node)
    }

    private fun processNode(node: Node) {
        when (node::class) {
            Spoiler::class -> processSpoilerNode(node as Spoiler)
            Strikethrough::class -> processStrikethroughNode(node as Strikethrough)
            Emphasis::class -> processEmphasisNode(node as Emphasis)
            StrongEmphasis::class -> processStrongEmphasisNode(node as StrongEmphasis)
            Underline::class -> processUnderlineNode(node as Underline)
            Code::class -> processCodeNode(node as Code)
            Text::class -> processTextNode(node as Text)
            else -> {}
        }
    }

    protected fun processNodeChildren(node: Node) {
        var child = node.firstChild
        while (child != null) {
            val next = child.next
            processNode(child)
            child = next
        }
    }

    protected abstract fun processSpoilerNode(node: Spoiler)
    protected abstract fun processStrikethroughNode(node: Strikethrough)
    protected abstract fun processEmphasisNode(node: Emphasis)
    protected abstract fun processStrongEmphasisNode(node: StrongEmphasis)
    protected abstract fun processUnderlineNode(node: Underline)
    protected abstract fun processCodeNode(node: Code)
    protected abstract fun processTextNode(node: Text)
}

