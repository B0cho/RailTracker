package com.b0cho.railtracker

import android.net.Uri
import com.google.gson.*
import java.lang.reflect.Type

/**
 * Copyrights to @rdeo-aziwell
 * https://gist.github.com/ypresto/3607f395ac4ef2921a8de74e9a243629?permalink_comment_id=5017254#gistcomment-5017254
 */
class UriAdapter : JsonDeserializer<Uri?>, JsonSerializer<Uri?> {
    override fun deserialize(json: JsonElement, type: Type?, context: JsonDeserializationContext?): Uri = runCatching {
        Uri.parse(json.asString)
    }.getOrDefault(Uri.EMPTY)

    override fun serialize(src: Uri?, type: Type?, context: JsonSerializationContext?):
            JsonElement =
        JsonPrimitive(src.toString())
}