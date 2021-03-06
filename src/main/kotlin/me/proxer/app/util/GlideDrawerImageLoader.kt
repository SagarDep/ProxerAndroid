package me.proxer.app.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import me.proxer.app.GlideApp

/**
 * @author Ruben Gees
 */
class GlideDrawerImageLoader : AbstractDrawerImageLoader() {

    override fun set(image: ImageView, uri: Uri?, placeholder: Drawable?, tag: String?) {
        GlideApp.with(image)
            .load(uri)
            .centerCrop()
            .placeholder(placeholder)
            .into(image)
    }

    override fun cancel(imageView: ImageView) = GlideApp.with(imageView).clear(imageView)

    override fun placeholder(context: Context, tag: String?): IconicsDrawable = IconicsDrawable(context)
        .icon(CommunityMaterial.Icon.cmd_account)
        .colorRes(android.R.color.white)
}
