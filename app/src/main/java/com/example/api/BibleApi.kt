package com.example.api

import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class BibleResponse(
    val data: BibleData?
)

@JsonClass(generateAdapter = true)
data class BibleData(
    val book: BibleBook?,
    val verses: List<BibleVerse>?
)

@JsonClass(generateAdapter = true)
data class BibleBook(
    val no: Int,
    val name: String,
    val chapter: Int
)

@JsonClass(generateAdapter = true)
data class BibleVerse(
    val verse: Int,
    val type: String, // "content" or "title"
    val content: String
)

interface BibleApiService {
    @GET("api/v1/passage/{book}/{chapter}")
    suspend fun getChapter(
        @Path("book") book: String,
        @Path("chapter") chapter: Int
    ): BibleResponse

    companion object {
        fun create(): BibleApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://beeble.vercel.app/")
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
            return retrofit.create(BibleApiService::class.java)
        }
    }
}

@JsonClass(generateAdapter = true)
data class KjvResponse(
    val reference: String?,
    val verses: List<KjvVerse>?
)

@JsonClass(generateAdapter = true)
data class KjvVerse(
    val book_name: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

interface KjvApiService {
    @GET("{query}")
    suspend fun getPassage(
        @Path("query", encoded = true) query: String,
        @Query("translation") translation: String = "kjv"
    ): KjvResponse

    companion object {
        fun create(): KjvApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://bible-api.com/")
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
            return retrofit.create(KjvApiService::class.java)
        }
    }
}
