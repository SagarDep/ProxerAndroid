package me.proxer.app.ui.view.bbcode.prototype

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.core.content.ContextCompat
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.util.extension.dip

/**
 * @author Ruben Gees
 */
object DividerPrototype : BBPrototype {

    override val startRegex = Regex(" *hr *", REGEX_OPTIONS)
    override val endRegex = Regex("/ *hr *", REGEX_OPTIONS)

    override val canHaveChildren get() = false

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        return listOf(View(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, dip(2))

            setBackgroundColor(ContextCompat.getColor(parent.context, R.color.divider))
        })
    }
}
