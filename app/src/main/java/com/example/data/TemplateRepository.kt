package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream

class TemplateRepository(private val context: Context) {

    private var cachedTemplates: List<BannerTemplate>? = null

    suspend fun getTemplates(): List<BannerTemplate> = withContext(Dispatchers.IO) {
        cachedTemplates?.let { return@withContext it }

        val list = mutableListOf<BannerTemplate>()
        try {
            val jsonString = context.assets.open("templates/catalog.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val array = root.optJSONArray("templates") ?: return@withContext emptyList()

            for (i in 0 until array.length()) {
                val tObj = array.getJSONObject(i)
                val id = tObj.getString("id")
                val name = tObj.getString("name")
                val category = tObj.optString("category", "General")
                val description = tObj.optString("description", "")
                val thumb = tObj.optString("thumbnailAsset", "")
                val bg = tObj.optString("backgroundAsset", "")

                val slotsList = mutableListOf<TemplateSlot>()
                val slotsArray = tObj.optJSONArray("slots")
                if (slotsArray != null) {
                    for (j in 0 until slotsArray.length()) {
                        val sObj = slotsArray.getJSONObject(j)
                        val slotId = sObj.getString("id")
                        val typeStr = sObj.optString("type", "text")
                        val label = sObj.getString("label")
                        val required = sObj.optBoolean("required", true)

                        val rectObj = sObj.getJSONObject("rect")
                        val rect = SlotRect(
                            x = rectObj.getInt("x"),
                            y = rectObj.getInt("y"),
                            width = rectObj.getInt("width"),
                            height = rectObj.getInt("height")
                        )

                        val styleObj = sObj.optJSONObject("defaultStyle")
                        val style = if (styleObj != null) {
                            SlotTextStyle(
                                fontSize = styleObj.optDouble("fontSize", 64.0).toFloat(),
                                colorHex = styleObj.optString("colorHex", "#FFFFFF"),
                                isBold = styleObj.optBoolean("isBold", true),
                                alignment = styleObj.optString("alignment", "CENTER")
                            )
                        } else null

                        slotsList.add(
                            TemplateSlot(
                                id = slotId,
                                type = TemplateSlotType.fromString(typeStr),
                                label = label,
                                rect = rect,
                                defaultStyle = style,
                                required = required
                            )
                        )
                    }
                }

                list.add(
                    BannerTemplate(
                        id = id,
                        name = name,
                        category = category,
                        description = description,
                        thumbnailAsset = thumb,
                        backgroundAsset = bg,
                        slots = slotsList
                    )
                )
            }
            cachedTemplates = list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load templates from catalog.json", e)
        }
        return@withContext list
    }

    suspend fun getTemplateById(id: String): BannerTemplate? {
        return getTemplates().firstOrNull { it.id == id }
    }

    suspend fun loadAssetBytes(assetPath: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            context.assets.open(assetPath).use { it.readBytes() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read asset bytes for $assetPath", e)
            null
        }
    }

    suspend fun loadAssetBitmap(assetPath: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap from asset: $assetPath", e)
            null
        }
    }

    companion object {
        private const val TAG = "TemplateRepository"
    }
}
