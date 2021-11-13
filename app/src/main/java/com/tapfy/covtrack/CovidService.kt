package com.tapfy.covtrack


import retrofit2.http.GET
import retrofit2.Call

interface CovidService{
    @GET("us/daily.json")
    fun getNationalData() : Call<List<CovidData>>

    @GET("states/daily.json")
    fun getStateData():Call<List<CovidData>>
}