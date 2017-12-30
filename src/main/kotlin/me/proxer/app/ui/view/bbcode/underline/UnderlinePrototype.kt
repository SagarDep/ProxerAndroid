package me.proxer.app.ui.view.bbcode.underline

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * @author Ruben Gees
 */
object UnderlinePrototype : BBPrototype {

    override val startRegex = Regex("\\s*u\\s*", IGNORE_CASE)
    override val endRegex = Regex("/\\s*u\\s*", IGNORE_CASE)

    override fun construct(code: String, parent: BBTree) = UnderlineTree(parent)
}
