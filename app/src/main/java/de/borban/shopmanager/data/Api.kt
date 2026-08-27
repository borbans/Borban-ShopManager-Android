package de.borban.shopmanager.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ShopApi {
    @POST("borban-shopmanager/api/v1/pair") suspend fun pair(@Body body: PairRequest): ApiEnvelope<PairResponse>
    @GET("borban-shopmanager/api/v1/me") suspend fun me(): ApiEnvelope<Map<String, Any>>
    @GET("borban-shopmanager/api/v1/dashboard") suspend fun dashboard(): ApiEnvelope<Dashboard>
    @GET("borban-shopmanager/api/v1/orders") suspend fun orders(@Query("limit") limit:Int=50,@Query("search") search:String=""): ApiEnvelope<List<OrderSummary>>
    @GET("borban-shopmanager/api/v1/orders/{id}") suspend fun order(@Path("id") id:String): ApiEnvelope<OrderDetail>
    @POST("borban-shopmanager/api/v1/orders/{id}/transition") suspend fun transition(@Path("id") id:String,@Body body:TransitionRequest): ApiEnvelope<Map<String,Boolean>>
    @POST("borban-shopmanager/api/v1/device/push-token") suspend fun pushToken(@Body body:PushTokenRequest): ApiEnvelope<Map<String,Boolean>>
}

object ApiFactory {
    private fun base(url:String) = url.trim().trimEnd('/') + "/"
    fun pairing(url:String):ShopApi = Retrofit.Builder().baseUrl(base(url)).addConverterFactory(GsonConverterFactory.create()).build().create(ShopApi::class.java)
    fun forShop(shop:ShopConnection):ShopApi {
        val client=OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("X-Borban-Device",shop.deviceId).header("Authorization","Bearer ${shop.token}").build())
        }).build()
        return Retrofit.Builder().baseUrl(base(shop.url)).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(ShopApi::class.java)
    }
}

class ShopStore(context: Context) {
    private val gson=Gson()
    private val master=androidx.security.crypto.MasterKey.Builder(context).setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs=androidx.security.crypto.EncryptedSharedPreferences.create(context,"borban_shops",master,androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    fun list():List<ShopConnection> { val raw=prefs.getString("shops","[]") ?: "[]"; return runCatching { gson.fromJson<List<ShopConnection>>(raw,object:TypeToken<List<ShopConnection>>(){}.type) }.getOrDefault(emptyList()) }
    fun save(shop:ShopConnection) { val all=list().filterNot{it.shopId==shop.shopId || it.url.equals(shop.url,true)}+shop; prefs.edit().putString("shops",gson.toJson(all)).apply() }
    fun remove(shopId:String) { prefs.edit().putString("shops",gson.toJson(list().filterNot{it.shopId==shopId})).apply() }
}
