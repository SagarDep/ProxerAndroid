package me.proxer.app.base

import android.annotation.TargetApi
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.TabLayout
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.transition.Transition
import android.view.MenuItem
import android.widget.ImageView
import com.bumptech.glide.request.target.ImageViewTarget
import com.h6ah4i.android.tablayouthelper.TabLayoutHelper
import com.jakewharton.rxbinding2.support.design.widget.offsetChanges
import com.jakewharton.rxbinding2.view.clicks
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.util.ActivityUtils
import me.proxer.app.view.ImageDetailActivity
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
abstract class ImageTabsActivity : BaseActivity() {

    abstract val headerImageUrl: HttpUrl?
    abstract val sectionsPagerAdapter: PagerAdapter

    open protected val contentView
        get() = R.layout.activity_image_tabs

    open protected val itemToDisplay
        get() = 0

    private var isHeaderImageVisible = true

    open protected val toolbar: Toolbar by bindView(R.id.toolbar)
    open protected val appbar: AppBarLayout by bindView(R.id.appbar)
    open protected val collapsingToolbar: CollapsingToolbarLayout by bindView(R.id.collapsingToolbar)
    open protected val viewPager: ViewPager by bindView(R.id.viewPager)
    open protected val headerImage: ImageView by bindView(R.id.image)
    open protected val tabs: TabLayout by bindView(R.id.tabs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(contentView)
        setSupportActionBar(toolbar)
        supportPostponeEnterTransition()

        setupToolbar()
        setupImage()
        loadImage()

        if (isEnterTransitionPossible(savedInstanceState) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.sharedElementEnterTransition.addListener(object : TransitionListenerWrapper {

                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                override fun onTransitionEnd(transition: Transition?) {
                    window.sharedElementEnterTransition.removeListener(this)

                    viewPager.postDelayed({
                        setupContent(savedInstanceState)
                    }, 50)
                }
            })
        } else {
            setupContent(savedInstanceState)
        }
    }

    override fun onBackPressed() = when (isHeaderImageVisible) {
        true -> super.onBackPressed()
        false -> finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                when (isHeaderImageVisible) {
                    true -> supportFinishAfterTransition()
                    false -> finish()
                }

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    open protected fun setupImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            headerImage.transitionName = ActivityUtils.getTransitionName(this)
        }

        headerImage.clicks()
                .bindToLifecycle(this)
                .subscribe {
                    val safeHeaderImageUrl = headerImageUrl

                    if (headerImage.drawable != null && safeHeaderImageUrl != null) {
                        if (ViewCompat.getTransitionName(headerImage) == null) {
                            ViewCompat.setTransitionName(headerImage, "header")
                        }

                        ImageDetailActivity.navigateTo(this@ImageTabsActivity, safeHeaderImageUrl, headerImage)
                    }
                }
    }

    open protected fun loadImage(animate: Boolean = true) {
        if (headerImageUrl == null) {
            loadEmptyImage()

            supportStartPostponedEnterTransition()
        } else {
            GlideApp.with(this)
                    .load(headerImageUrl.toString())
                    .into(object : ImageViewTarget<Drawable>(headerImage) {
                        override fun setResource(resource: Drawable?) {
                            headerImage.setImageDrawable(resource)

                            if (resource != null) {
                                supportStartPostponedEnterTransition()
                            }
                        }
                    })
        }
    }

    open protected fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        collapsingToolbar.isTitleEnabled = false

        appbar.offsetChanges()
                .bindToLifecycle(this)
                .subscribe {
                    isHeaderImageVisible = collapsingToolbar.height + it > collapsingToolbar.scrimVisibleHeightTrigger
                }
    }

    open protected fun setupContent(savedInstanceState: Bundle?) {
        viewPager.adapter = sectionsPagerAdapter

        if (savedInstanceState == null) {
            viewPager.currentItem = itemToDisplay
        }

        TabLayoutHelper(tabs, viewPager).apply { isAutoAdjustTabModeEnabled = true }
    }

    open protected fun loadEmptyImage() = Unit

    private fun isEnterTransitionPossible(savedInstanceState: Bundle?): Boolean {
        return savedInstanceState == null && ActivityUtils.getTransitionName(this) != null
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    interface TransitionListenerWrapper : Transition.TransitionListener {
        override fun onTransitionEnd(transition: Transition?) = Unit
        override fun onTransitionResume(transition: Transition?) = Unit
        override fun onTransitionPause(transition: Transition?) = Unit
        override fun onTransitionCancel(transition: Transition?) = Unit
        override fun onTransitionStart(transition: Transition?) = Unit
    }
}