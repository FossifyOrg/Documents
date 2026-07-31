package org.fossify.documents.models

import org.json.JSONObject

data class DocumentEntry(
    val uri: String,
    val name: String,
    val mimeType: String,
    val kind: DocumentKind,
    val location: String,
    val size: Long?,
    val lastModified: Long?,
    val lastOpened: Long,
    val lastPage: Int = 0,
    val isFavorite: Boolean = false,
) {
    fun toJson() = JSONObject().apply {
        put(KEY_URI, uri)
        put(KEY_NAME, name)
        put(KEY_MIME_TYPE, mimeType)
        put(KEY_LOCATION, location)
        put(KEY_SIZE, size)
        put(KEY_LAST_MODIFIED, lastModified)
        put(KEY_LAST_OPENED, lastOpened)
        put(KEY_LAST_PAGE, lastPage)
        put(KEY_IS_FAVORITE, isFavorite)
    }

    companion object {
        private const val KEY_URI = "uri"
        private const val KEY_NAME = "name"
        private const val KEY_MIME_TYPE = "mime_type"
        private const val KEY_LOCATION = "location"
        private const val KEY_SIZE = "size"
        private const val KEY_LAST_MODIFIED = "last_modified"
        private const val KEY_LAST_OPENED = "last_opened"
        private const val KEY_LAST_PAGE = "last_page"
        private const val KEY_IS_FAVORITE = "is_favorite"

        fun fromJson(json: JSONObject): DocumentEntry? {
            val uri = json.optString(KEY_URI)
            val name = json.optString(KEY_NAME, uri.substringAfterLast('/'))
            val mimeType = json.optString(KEY_MIME_TYPE)
            val kind = DocumentKind.fromName(name, mimeType)

            return if (uri.isBlank() || kind == DocumentKind.OTHER) {
                null
            } else {
                DocumentEntry(
                    uri = uri,
                    name = name,
                    mimeType = mimeType,
                    kind = kind,
                    location = json.optString(KEY_LOCATION),
                    size = json.optNullableLong(KEY_SIZE),
                    lastModified = json.optNullableLong(KEY_LAST_MODIFIED),
                    lastOpened = json.optLong(KEY_LAST_OPENED, 0L),
                    lastPage = json.optInt(KEY_LAST_PAGE, 0),
                    isFavorite = json.optBoolean(KEY_IS_FAVORITE, false),
                )
            }
        }

        private fun JSONObject.optNullableLong(key: String): Long? {
            return if (has(key) && !isNull(key)) optLong(key) else null
        }
    }
}
