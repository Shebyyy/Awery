package eu.kanade.tachiyomi.source.model

import com.mrboomdev.awery.utils.ExtensionSdk
import java.io.Serializable

@ExtensionSdk
interface SManga : Serializable {
    var url: String
    var title: String
    var artist: String?
    var author: String?
    var description: String?
    var genre: String?
    var status: Int
    var thumbnail_url: String?
    var update_strategy: UpdateStrategy
    var initialized: Boolean

    companion object {
        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3
        const val PUBLISHING_FINISHED = 4
        const val CANCELLED = 5
        const val ON_HIATUS = 6

        @ExtensionSdk
        fun create(): SManga = object : SManga {
            override lateinit var url: String
            override lateinit var title: String
            override var artist: String? = null
            override var author: String? = null
            override var description: String? = null
            override var genre: String? = null
            override var status: Int = 0
            override var thumbnail_url: String? = null
            override var update_strategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE
            override var initialized: Boolean = false
        }
    }
}