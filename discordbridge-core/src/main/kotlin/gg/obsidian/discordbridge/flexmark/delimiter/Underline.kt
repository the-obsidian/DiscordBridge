package gg.obsidian.discordbridge.flexmark.delimiter

import com.vladsch.flexmark.ast.DelimitedNodeImpl
import com.vladsch.flexmark.util.sequence.BasedSequence

class Underline(openingMarker: BasedSequence, text: BasedSequence, closingMarker: BasedSequence) :
    DelimitedNodeImpl(openingMarker, text, closingMarker)