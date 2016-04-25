package com.proxerme.app.manager;

import android.support.annotation.Nullable;

import com.proxerme.app.util.helper.MaterialDrawerHelper;
import com.proxerme.app.util.helper.PagingHelper;
import com.proxerme.app.util.helper.StorageHelper;
import com.proxerme.library.event.success.ConferencesEvent;
import com.proxerme.library.event.success.NewsEvent;
import com.proxerme.library.util.ProxerInfo;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;

import static com.proxerme.app.util.helper.MaterialDrawerHelper.DrawerItemId;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public class BadgeManager extends StarteableManager {

    private WeakReference<BadgeCallback> callback = new WeakReference<>(null);

    public BadgeManager() {
        super();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewsLoaded(NewsEvent event) {
        if (callback.get() != null) {
            callback.get().updateBadge(MaterialDrawerHelper.DRAWER_ID_NEWS, null);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConferencesLoaded(ConferencesEvent event) {
        if (callback != null) {
            callback.get().updateBadge(MaterialDrawerHelper.DRAWER_ID_MESSAGES, null);
        }
    }

    private void init() {
        if (callback.get() != null) {
            int newNews = StorageHelper.getNewNews();
            int newMessages = StorageHelper.getNewMessages();

            if (newNews > 0 || newNews == PagingHelper.OFFSET_NOT_CALCULABLE) {
                callback.get().updateBadge(MaterialDrawerHelper.DRAWER_ID_NEWS,
                        newNews == PagingHelper.OFFSET_NOT_CALCULABLE ?
                                (ProxerInfo.NEWS_ON_PAGE + "+") : (String.valueOf(newNews)));
            }

            if (newMessages > 0) {
                callback.get().updateBadge(MaterialDrawerHelper.DRAWER_ID_MESSAGES,
                        String.valueOf(newMessages));
            }
        }
    }

    public void destroy() {
        this.callback = null;
    }

    public void setCallback(@Nullable BadgeCallback callback) {
        this.callback = new WeakReference<BadgeCallback>(callback);

        init();
    }

    public interface BadgeCallback {
        void updateBadge(@DrawerItemId int id, @Nullable String count);
    }

}
