package com.knigdelioglu.puanla.domain.group

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Pure JSON mutation used inside a Room transaction, never on stale UI copies. */
object GroupChecklistRules {
    fun toggle(source: String, position: Int): String {
        val items = Json.parseToJsonElement(source).jsonArray
        require(position in items.indices) { "Grup görevi dizini geçersiz." }
        val modified = items.mapIndexed { index, item ->
            if (index != position) item else {
                val obj = item.jsonObject
                val checked = obj["checked"]?.jsonPrimitive?.booleanOrNull
                    ?: throw IllegalArgumentException("Grup görevi onay durumu geçersiz.")
                JsonObject(obj + ("checked" to JsonPrimitive(!checked)))
            }
        }
        return JsonArray(modified).toString()
    }
}
