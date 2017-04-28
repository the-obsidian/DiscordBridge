package gg.obsidian.discordbridge.utils

import org.parboiled.common.Preconditions
import org.pegdown.Printer
import org.pegdown.ast.*

class MarkdownToMinecraftSeralizer : Visitor {

    private var printer = Printer()

    fun toMinecraft(astRoot: RootNode): String {
        Preconditions.checkArgNotNull<Any>(astRoot, "astRoot")
        astRoot.accept(this)
        return printer.string
    }

    override fun visit(node: RootNode) {
        visitChildren(node)
    }

    override fun visit(node: AnchorLinkNode) {
        return
    }

    override fun visit(node: AutoLinkNode) {
        printer.print(node.text)
    }

    override fun visit(node: BlockQuoteNode) {
        visitChildren(node)
    }

    override fun visit(node: BulletListNode) {
        visitChildren(node)
    }

    override fun visit(node: CodeNode) {
        printer.print(node.text)
    }

    override fun visit(node: DefinitionListNode) {
        visitChildren(node)
    }

    override fun visit(node: DefinitionNode) {
        visitChildren(node)
    }

    override fun visit(node: DefinitionTermNode) {
        visitChildren(node)
    }

    override fun visit(node: ExpImageNode) {
        visitChildren(node)
    }

    override fun visit(node: ExpLinkNode) {
        visitChildren(node)
    }

    override fun visit(node: HeaderNode) {
        visitChildren(node)
    }

    override fun visit(node: HtmlBlockNode) {
        printer.print(node.text)
    }

    override fun visit(node: InlineHtmlNode) {
        printer.print(node.text)
    }

    override fun visit(node: ListItemNode) {
        visitChildren(node)
    }

    override fun visit(node: MailLinkNode) {
        printer.print(node.text)
    }

    override fun visit(node: OrderedListNode) {
        visitChildren(node)
    }

    override fun visit(node: ParaNode) {
        visitChildren(node)
    }

    override fun visit(node: QuotedNode) {
        visitChildren(node)
    }

    override fun visit(node: ReferenceNode) {
        visitChildren(node)
    }

    override fun visit(node: RefImageNode) {
        visitChildren(node)
    }

    override fun visit(node: RefLinkNode) {
        visitChildren(node)
    }

    override fun visit(node: SimpleNode) {
        when (node.type) {
            SimpleNode.Type.Apostrophe -> printer.print("'")
            SimpleNode.Type.Ellipsis -> printer.print("...")
            SimpleNode.Type.Emdash -> printer.print("--")
            SimpleNode.Type.Endash -> printer.print("-")
            SimpleNode.Type.HRule -> return
            SimpleNode.Type.Linebreak -> return
            SimpleNode.Type.Nbsp -> return
            else -> return
        }
    }

    override fun visit(node: SpecialTextNode) {
        printer.print(node.text)
    }

    override fun visit(node: StrikeNode) {
        printTag(node, "\u00A7m")
    }

    override fun visit(node: StrongEmphSuperNode) {
        if (node.isClosed) {
            if (node.isStrong) {
                if (node.chars == "**")
                    printTag(node, "\u00A7l")
                else if (node.chars == "__")
                    printTag(node, "\u00A7n")
            }
            else
                printTag(node, "\u00A7o")
        } else {
            //sequence was not closed, treat open chars as ordinary chars
            printer.print(node.chars)
            visitChildren(node)
        }
    }

    override fun visit(node: TableBodyNode) {
        visitChildren(node)
    }

    override fun visit(node: TableCaptionNode) {
        visitChildren(node)
    }

    override fun visit(node: TableCellNode) {
        visitChildren(node)
    }

    override fun visit(node: TableColumnNode) {
        visitChildren(node)
    }

    override fun visit(node: TableHeaderNode) {
        visitChildren(node)
    }

    override fun visit(node: TableNode) {
        visitChildren(node)
    }

    override fun visit(node: TableRowNode) {
        visitChildren(node)
    }

    override fun visit(node: VerbatimNode) {
        printer.print(node.text)
    }

    override fun visit(node: WikiLinkNode) {
        printer.print(node.text)
    }

    override fun visit(node: TextNode) {
        printer.print(node.text)
    }

    override fun visit(node: SuperNode) {
        visitChildren(node)
    }

    override fun visit(node: Node) {
        return
    }

    override fun visit(node: AbbreviationNode) {
        visitChildren(node)
    }

    // helpers
    private fun visitChildren(node: SuperNode, tag: String) {
        for ((index, child) in node.children.withIndex()) {
            child.accept(this)
            if (index != node.children.size - 1)
                printer.print(tag)
        }
    }

    private fun visitChildren(node: SuperNode) {
        for (child in node.children) {
            child.accept(this)
        }
    }

    private fun printTag(node: SuperNode, tag: String) {
        printer.print(tag)
        visitChildren(node, tag)
        printer.print("\u00A7r")
    }

}