package com.example.db

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONException

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        val array = JSONArray()
        for (item in value) {
            array.put(item)
        }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return list
    }

    companion object {
        fun serializeContentList(items: List<com.example.model.PresentationContent>): String {
            val arr = JSONArray()
            for (item in items) {
                val obj = org.json.JSONObject()
                obj.put("id", item.id)
                obj.put("title", item.title)
                when (item) {
                    is com.example.model.LyricsContent -> {
                        obj.put("type", "LYRICS")
                        val slidesArr = JSONArray()
                        item.slides.forEach { slidesArr.put(it) }
                        obj.put("slides", slidesArr)
                    }
                    is com.example.model.BibleContent -> {
                        obj.put("type", "BIBLE")
                        obj.put("bookAndChapter", item.bookAndChapter)
                        val versesArr = JSONArray()
                        item.verses.forEach { versesArr.put(it) }
                        obj.put("verses", versesArr)
                    }
                    is com.example.model.VideoContent -> {
                        obj.put("type", "VIDEO")
                        obj.put("uri", item.uri)
                    }
                    is com.example.model.ImageContent -> {
                        obj.put("type", "IMAGE")
                        obj.put("uri", item.uri)
                    }
                    is com.example.model.CameraContent -> {
                        obj.put("type", "CAMERA")
                        obj.put("cameraId", item.cameraId)
                    }
                    is com.example.model.IpCameraContent -> {
                        obj.put("type", "IP_CAMERA")
                        obj.put("streamUrl", item.streamUrl)
                    }
                    is com.example.model.PowerPointContent -> {
                        obj.put("type", "POWERPOINT")
                        val slidesArr = JSONArray()
                        item.slides.forEach { slidesArr.put(it) }
                        obj.put("slides", slidesArr)
                    }
                }
                arr.put(obj)
            }
            return arr.toString()
        }

        fun deserializeContentList(json: String?): List<com.example.model.PresentationContent> {
            if (json.isNullOrBlank()) return emptyList()
            val result = mutableListOf<com.example.model.PresentationContent>()
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("id", System.currentTimeMillis().toString())
                    val title = obj.optString("title", "Untitled")
                    val type = obj.optString("type", "LYRICS")
                    when (type) {
                        "LYRICS" -> {
                            val slidesArr = obj.optJSONArray("slides")
                            val slides = mutableListOf<String>()
                            if (slidesArr != null) {
                                for (s in 0 until slidesArr.length()) {
                                    slides.add(slidesArr.getString(s))
                                }
                            }
                            result.add(com.example.model.LyricsContent(id, title, slides))
                        }
                        "BIBLE" -> {
                            val bookAndChapter = obj.optString("bookAndChapter", title)
                            val versesArr = obj.optJSONArray("verses")
                            val verses = mutableListOf<String>()
                            if (versesArr != null) {
                                for (v in 0 until versesArr.length()) {
                                    verses.add(versesArr.getString(v))
                                }
                            }
                            result.add(com.example.model.BibleContent(id, title, bookAndChapter, verses))
                        }
                        "VIDEO" -> {
                            result.add(com.example.model.VideoContent(id, title, obj.optString("uri", "")))
                        }
                        "IMAGE" -> {
                            result.add(com.example.model.ImageContent(id, title, obj.optString("uri", "")))
                        }
                        "CAMERA" -> {
                            result.add(com.example.model.CameraContent(id, title, obj.optString("cameraId", "0")))
                        }
                        "IP_CAMERA" -> {
                            result.add(com.example.model.IpCameraContent(id, title, obj.optString("streamUrl", "")))
                        }
                        "POWERPOINT" -> {
                            val slidesArr = obj.optJSONArray("slides")
                            val slides = mutableListOf<String>()
                            if (slidesArr != null) {
                                for (s in 0 until slidesArr.length()) {
                                    slides.add(slidesArr.getString(s))
                                }
                            }
                            result.add(com.example.model.PowerPointContent(id, title, slides))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return result
        }
    }
}
