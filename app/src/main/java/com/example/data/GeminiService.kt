package com.example.data

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(val text: String)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiPart>)

@JsonClass(generateAdapter = true)
data class GeminiRequest(val contents: List<GeminiContent>)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiContent)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun getTutorResponse(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}

object GeminiTutor {
    suspend fun fetchExplanation(clef: String, pitchName: String, japaneseName: String): String {
        val apiKey = try {
            com.example.BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "【通知】APIキーが設定されていません。AI講師の解説（Gemini）を利用するには、AI Studioの「Secrets」パネルに有効な『GEMINI_API_KEY』を入力してください。\n\n【ヒント】\n${clef}の「${japaneseName}」です。五線譜の位置をじっくり観察し、繰り返し練習して頭に焼き付けましょう！"
        }
        
        val systemPrompt = "あなたは親切な音楽学校のピアノ講師「AI先生」です。楽譜が読めるようになりたい初心者に向けて、特定の音符を覚えるための極めて分かりやすい『ビジュアル的なコツ』『覚え方の語呂合わせ (Mnemonic)』『五線譜の基準線（第1線や第3線など）との関係』を親しみやすく教えてください。返答は100文字〜150文字以内の温かみのある日本語（「〜ですよ」「〜ましょう」といった口調）にしてください。音階は「${japaneseName}」として言及してください。"
        val userPrompt = "$clef の音符「$pitchName ($japaneseName)」を早く覚えるためのビジュアル的なコツは何ですか？"
        
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = "$systemPrompt\n\n質問: $userPrompt")
                    )
                )
            )
        )
        
        return try {
            val response = GeminiClient.service.getTutorResponse(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            text ?: "解説を生成できませんでした。もう一度お試しください。"
        } catch (e: Exception) {
            "解説を取得できませんでした（${e.localizedMessage ?: "通信エラー"}).\n\n【ヒント】\n${clef}の「${japaneseName}」です。"
        }
    }
}
