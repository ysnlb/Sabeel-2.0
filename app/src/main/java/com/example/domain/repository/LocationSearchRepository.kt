package com.example.domain.repository

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class PhotonResponse(
    val features: List<Feature>
)

@JsonClass(generateAdapter = true)
data class Feature(
    val properties: Properties,
    val geometry: Geometry
)

@JsonClass(generateAdapter = true)
data class Properties(
    val name: String? = null,
    val city: String? = null,
    val country: String? = null,
    val state: String? = null
)

@JsonClass(generateAdapter = true)
data class Geometry(
    val type: String,
    val coordinates: List<Double> // [lon, lat]
)

data class LocationResult(
    val name: String,
    val lat: Double,
    val lon: Double
)

interface PhotonApiService {
    @GET("api/")
    suspend fun searchCity(@Query("q") query: String, @Query("limit") limit: Int = 10): PhotonResponse
}

class LocationSearchRepository {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://photon.komoot.io/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(PhotonApiService::class.java)

    suspend fun searchLocations(query: String): List<LocationResult> {
        return try {
            val response = api.searchCity(query)
            response.features.mapNotNull { feature ->
                val name = feature.properties.city ?: feature.properties.name ?: return@mapNotNull null
                val coords = feature.geometry.coordinates
                if (coords.size >= 2) {
                    val country = feature.properties.country?.let { ", $it" } ?: ""
                    LocationResult(
                        name = "$name$country",
                        lat = coords[1], // latitude is 2nd in GeoJSON
                        lon = coords[0]  // longitude is 1st in GeoJSON
                    )
                } else null
            }.distinctBy { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
