package org.fossify.documents.models

import org.json.JSONObject

data class DocumentFolder(
    val uri: String,
    val name: String,
    val itemCount: Int? = null,
    val lastOpened: Long = 0L,
) {
    fun toJson() = JSONObject().apply {
        put(KEY_URI, uri)
        put(KEY_NAME, name)
        itemCount?.let { put(KEY_ITEM_COUNT, it) }
        put(KEY_LAST_OPENED, lastOpened)
    }

    companion object {
        private const val KEY_URI = "uri"
        private const val KEY_NAME = "name"
        private const val KEY_ITEM_COUNT = "item_count"
        private const val KEY_DOCUMENT_COUNT = "document_count"
        private const val KEY_LAST_OPENED = "last_opened"

        fun fromJson(json: JSONObject): DocumentFolder? {
            val uri = json.optString(KEY_URI).takeIf { it.isNotBlank() } ?: return null
            return DocumentFolder(
                uri = uri,
                name = json.optString(KEY_NAME, uri.substringAfterLast('/')),
                itemCount = when {
                    json.has(KEY_ITEM_COUNT) && !json.isNull(KEY_ITEM_COUNT) -> json.optInt(KEY_ITEM_COUNT)
                    json.has(KEY_DOCUMENT_COUNT) && !json.isNull(KEY_DOCUMENT_COUNT) -> json.optInt(KEY_DOCUMENT_COUNT)
                    else -> null
                },
                lastOpened = json.optLong(KEY_LAST_OPENED, 0L),
            )
        }
    }
}
