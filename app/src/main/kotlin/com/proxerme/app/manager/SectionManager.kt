package com.proxerme.app.manager

import com.proxerme.app.event.SectionChangedEvent
import org.greenrobot.eventbus.EventBus
import kotlin.properties.Delegates

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object SectionManager {
    enum class Section {
        NONE, NEWS, PROFILE, TOPTEN, CONFERENCES, CHAT, USER_MEDIA_LIST, MEDIA_LIST,
        CONFERENCE_INFO, NEW_CHAT, HISTORY, UCP_OVERVIEW, MEDIA_INFO, REMINDER, COMMENTS, RELATIONS,
        EPISODES, MANGA, ANIME, TRANSLATOR_GROUP_INFO, INDUSTRY_INFO, TRANSLATOR_GROUP_PROJECTS,
        INDUSTRY_PROJECTS
    }

    var currentSection: Section by Delegates.observable(Section.NONE, { _, _, new ->
        EventBus.getDefault().post(SectionChangedEvent(new))
    })
}