package com.tapfy.covtrack

import android.opengl.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.ticker.TickerUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.tapfy.covtrack.CovidService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.android.synthetic.main.activity_main.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


private const val BASE_URL = "https://api.covidtracking.com/v1/"
private const val TAG = "MainActivity"
private const val ALL_STATES: String = "All (Nation wide)"
class MainActivity : AppCompatActivity() {

    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tickerView.setCharacterLists(TickerUtils.provideNumberList())


        val gson =GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit: Retrofit = Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val covidService = retrofit.create(CovidService::class.java)

        //fetch nat data
         covidService.getNationalData().enqueue(object:Callback<List<CovidData>>{
             override fun onResponse(
                 call: Call<List<CovidData>>,
                 response: Response<List<CovidData>>
             ) {
                 Log.i(TAG,"onResponse $response")
              val nationalData = response.body()
                 if (nationalData == null){
                     Log.w(TAG,"no valid response")
                     return
                 }
               nationalDailyData = nationalData.reversed()

                Log.i(TAG,"update graph with nat data")
                 updateDisplayWithData(nationalDailyData)

             }

             override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG,"onFailiur $t")
             }

         })
        //fetch state data
          covidService.getStateData().enqueue(object:Callback<List<CovidData>>{
              override fun onResponse(
                  call: Call<List<CovidData>>,
                  response: Response<List<CovidData>>
              ) {
                  setupEventListeners()
                  Log.i(TAG,"onResponse $response")
                  val statesData = response.body()
                  if (statesData == null){
                      Log.w(TAG,"no valid response")
                      return
                  }
                 perStateDailyData = statesData.reversed().groupBy { it.state }

                  Log.i(TAG,"update graph with state data")
                  updateSpinnerWithStateData(perStateDailyData.keys)

              }

              override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                  Log.e(TAG,"onFailiur $t")
              }

          })


    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
      val stateAbbreviationList = stateNames.toMutableList()
        stateAbbreviationList.sort()
        stateAbbreviationList.add(0,ALL_STATES)


        spinnerSelect.attachDataSource(stateAbbreviationList)
      spinnerSelect.setOnSpinnerItemSelectedListener{parent,_,position,_ ->
          val selectedState = parent.getItemAtPosition(position) as String
         val selectedData =  perStateDailyData[selectedState] ?: nationalDailyData
          updateDisplayWithData(selectedData)
      }

    }

    private fun setupEventListeners() {



        // scrubing lister
        sparkView.isScrubEnabled = true
        sparkView.setScrubListener { itemData->
            if (itemData is CovidData){
               updateInfoForDate(itemData)
            }
        }

        // radio button selecter
        radioGroupTimeFrame.setOnCheckedChangeListener{ _, checkedId ->
            adapter.daysAgo = when(checkedId){
                R.id.radioButtonWeek -> TimeScale.WEEK
                R.id.radioButtonMonth-> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            adapter.notifyDataSetChanged()
        }
        radioGroupType.setOnCheckedChangeListener{_,checkedId->
            when(checkedId){
                R.id.radioButtonPosetive -> updateDisplyaMatric(Metric.POSITIVE)
                R.id.radioButtonNegative -> updateDisplyaMatric(Metric.NEGATIVE)
                R.id.radioButtonDeath -> updateDisplyaMatric(Metric.DEATH)
            }
        }
    }

    private fun updateDisplyaMatric(metric: Metric) {
        val colorRes = when(metric){
            Metric.NEGATIVE -> R.color.colorNegative
            Metric.POSITIVE -> R.color.colorPositive
            Metric.DEATH -> R.color.colorDeath
        }
        @ColorInt val colorInt = ContextCompat.getColor(this,colorRes)
        sparkView.lineColor = colorInt
        tickerView.setTextColor(colorInt)


        adapter.metric = metric
        adapter.notifyDataSetChanged()

        updateInfoForDate(currentlyShownData.last())

    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData
     //adding sparke

         adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        //update default to radio button
        radioButtonPosetive.isChecked = true
        radioButtonMax.isChecked = true
        //dislplay metrics for most recent date
        updateDisplyaMatric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when(adapter.metric){
            Metric.NEGATIVE-> covidData.negativeIncrease
            Metric.POSITIVE->covidData.positiveIncrease
            Metric.DEATH -> covidData.deathIncrease
        }

      tickerView.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd,yyyy", Locale.US)
      tvDate.text = outputDateFormat.format(covidData.dateChecked)
    }


}