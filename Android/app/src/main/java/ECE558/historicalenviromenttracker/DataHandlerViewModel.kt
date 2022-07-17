package ECE558.historicalenviromenttracker

import android.util.Log
import androidx.lifecycle.ViewModel
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ScanRequest
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import kotlinx.coroutines.runBlocking

import java.util.*

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.properties.Delegates
import kotlin.random.Random
import kotlin.math.sqrt


const val TIME_START = 1647300000000L

fun ServerTrackerFilter(message: String){
    Log.i("SERVER_TRACKER", message)}

fun StateTrackerFilter(message: String){
    Log.i("STATE_TRACKER", message)}


fun TimeInputOutputErrorFilter(message: String){
    Log.i("TIME_INPUT_OUTPUT", message)}

fun MathTrackerFilter(message: String){
    Log.i("MATH_TRACKER", message)}

fun MathErrorFilter(message: String){
    Log.i("MATH_Error", message)}
class DataHandlerPasser(){
    companion object{
        //val newInstace = DataHandlerPasser()
        val dataHandlerViewModel = DataHandlerViewModel()
    }
}
class DataHandlerViewModel() : ViewModel() {


    companion object {
        val dummyDataGenerator = dataGenerator()
    }
    lateinit var dataChart: AAChartModel

    //TODO Use these instance versions of the datasets as the versions whose range is going to be controlled by Time1 and Time2
    // That actually makes these convinent and useful as it already would allow for seperate ranging for the calcualtions and
    // graphs since they are being don eby different instances
    var temperatureData = perTypeDataSet("Temperature")
    var humidityData =perTypeDataSet("Humidity")
    var tvocData =perTypeDataSet("TVOC")
    var pressureData =perTypeDataSet("Pressure")
    var lightData =perTypeDataSet("Light")
    var co2Data =perTypeDataSet("CO2")
    val dataSetArray = arrayOf(temperatureData,humidityData, tvocData, pressureData, lightData, co2Data)
    val dataBase = ModelDatabase(this)
    var chartElementBools = Array<Boolean>(dataSetArray.size){true}
    var graphMinimumTime: Long = TIME_START
    var graphMaximumTime: Long = Long.MAX_VALUE

    init{
        this.clearData()
        this.checkServerAndProcessData()
    }


    fun checkServerAndProcessData(){
        this.dataBase.scanData()
        this.processData()

    }

    fun clearData(){
        for(i in dataSetArray.indices) {
            dataSetArray[i].reset()
            dataSetArray[i].graphMinimumTime= TIME_START
            dataSetArray[i].graphMaximumTime= Long.MAX_VALUE
            dataSetArray[i].calculationStartTime= TIME_START
            dataSetArray[i].calculationStopTime= Long.MAX_VALUE

        }
        this.dataBase.timeSampleValueToStartScan = TIME_START

    }    //TODO Need a way of using Date Strings or for the x axis.  Current graphing system does not like my attempts at using date string




    fun generateNewDummyDataSetForInstance(){
        for(i in dataSetArray.indices){
            dataSetArray[i].reset()
            dataSetArray[i].loadData(dummyDataGenerator.generateCyclicData(4*3,4))
        }

    }
    fun loadDummyDataPointForInstance(){
        for(i in dataSetArray.indices){
            dataSetArray[i].mockDataPointLoad(dummyDataGenerator.generateCyclicData(2,4)[1])
        }
    }




    fun processData(){
        for(i in dataSetArray.indices){
            dataSetArray[i].process()
        }

        var chartArray: Array<perTypeDataSet> = Array<perTypeDataSet>(chartElementBools.count{it}){perTypeDataSet("Dummy")}
        var j: Int = 0
        for (i in dataSetArray.indices){
            if(chartElementBools[i]){
                chartArray[j]=dataSetArray[i]
                j += 1
            }
        }

        generateChart(Array<perTypeDataSet>(chartArray.size){chartArray[it]}, "Mega Graph")

    }

    fun generateChart(dataTypes: Array<perTypeDataSet>, graphName: String) {
        var graphSubTitle: String = ""
        var chartElements: Array<AASeriesElement> = Array<AASeriesElement>(dataTypes.size){dataTypes[it].chartElement}
        for (i in 0..dataTypes.size-1){
            if(chartElementBools[i]) {
                graphSubTitle += dataTypes[i].dataName+" "
            }
            //if(i < (dataTypes.size-1)){ graphSubTitle+=" "}
        }
        dataChart= AAChartModel()
            .chartType(AAChartType.Spline)
            .title(graphName)
            .subtitle(graphSubTitle)
            .backgroundColor("#DDEEFF")
            .dataLabelsEnabled(true)
            .series(chartElements)
    }

    fun setGraphMaxTime(time: Long){
        for(i in dataSetArray.indices){
            dataSetArray[i].graphMaximumTime= time
        }
        this.graphMaximumTime=time
    }

    fun setGraphMinTime(time: Long){
        for(i in dataSetArray.indices){
            dataSetArray[i].graphMinimumTime= time
        }
        this.graphMinimumTime=time
    }


}
class perTypeDataSet(name :String) {
    val dataName: String = name
    private var dataList: MutableList<Array<Double>>? = null  //Previously I had left this uninitialized and was using ::dataList.isInitialized()

    var calculationDataList: MutableList<Array<Double>>? = null
    var graphDataList: MutableList<Array<Double>>? = null


    var dataMean by Delegates.notNull<Double>()
    var dataMedian by Delegates.notNull<Double>()
    var dataMode by Delegates.notNull<Double>()
    var dataMaximum by Delegates.notNull<Double>()
    var dataMinimum by Delegates.notNull<Double>()
    var dataSD by Delegates.notNull<Double>()

    var graphMinimumTime: Long = TIME_START
    var graphMaximumTime: Long = Long.MAX_VALUE

    var calculationStartTime: Long = TIME_START
    var calculationStopTime: Long = Long.MAX_VALUE

    lateinit var chartElement: AASeriesElement




    fun reset(){
        dataList= null
        calculationDataList=null
        graphDataList=null
    }

    fun isEmpty():Boolean{
        return dataList.isNullOrEmpty()
    }
    fun process(){



        if(!dataList.isNullOrEmpty()) {

            calculationDataList = MutableList<Array<Double>>(dataList!!.size) {
                arrayOf(
                    dataList!![it][0],
                    dataList!![it][1]
                )
            }
            graphDataList = MutableList<Array<Double>>(dataList!!.size) {
                arrayOf(
                    dataList!![it][0],
                    dataList!![it][1]
                )
            }
            calculationDataList!!.removeAll{(it[0]<calculationStartTime) or (it[0] > calculationStopTime)}
            MathTrackerFilter("Calculation Data Filter for $dataName leaving ${calculationDataList!!.size} Points")
            graphDataList!!.removeAll{(it[0]<graphMinimumTime) or (it[0] > graphMaximumTime)}
            MathTrackerFilter("Graph Data Filter for $dataName leaving ${graphDataList!!.size} Points")


            calculateMeanAndSD()
            MathTrackerFilter("Mean and SD were Calculated for $dataName")

            calculateMedianMaxMin()
            MathTrackerFilter("Median Max Min were Calculated for $dataName")

            calculateMode()
            MathTrackerFilter("Mode was Calculated for $dataName")

            generateChartElement()
        }
    }


    fun generateChartElement(){
        if(!graphDataList.isNullOrEmpty()){
            sortGraphData()
            chartElement = AASeriesElement()
                .name(dataName)

                .data(
                    Array(graphDataList!!.size)
                    {
                        arrayOf(graphDataList!![it][0], graphDataList!![it][1])
                    }
                )
        }
        else {
            chartElement = AASeriesElement()
                .name(dataName)
                .data(
                    Array(1)
                    {
                        arrayOf(0L, 0.0)
                    }
                )

        }
    }

    fun sortGraphData(){
        if(!graphDataList.isNullOrEmpty()){
            graphDataList!!.sortWith(compareBy{it[0]})
        }

    }



    fun loadData(datas: Array<Array<Double>>) {
        //Sabaton("Loading Array of Arrays of Doubles")
        for (i in datas.indices) {
            if (datas[i].size != 2) {

                return
            }
        }
        //Sabaton("There are ${datas.size} data points ")
        if(dataList.isNullOrEmpty()){
            //Sabaton("Initial Values to dataList")
            dataList= datas.toMutableList()
        } else{
            for (i in datas.indices) {
                dataList!!.add(datas[i])
            }
        }
    }

    fun loadData(timePoints: Array<Double>, dataPoints: Array<Double>) {
        if (dataPoints.size == timePoints.size) {
            for (i in dataPoints.indices) {
                loadData(arrayOf(arrayOf<Double>(timePoints[i], dataPoints[i])))
            }
        }
    }



    fun mockDataPointLoad(data: Array<Double>) {
        if (data.size != 2) {
            return
        } else{
            if(!dataList.isNullOrEmpty()){
                val newTime= this.dataList!!.last()[0]  + data[0]
                loadData(arrayOf(arrayOf(newTime, data[1])))
            }
        }
    }
    private fun calculateMeanAndSD() {
        var tempSum: Double = 0.0

        var  tempValue: Double = 0.0

        if(!calculationDataList.isNullOrEmpty()) {
            for (i in calculationDataList!!.indices) {
                tempSum += calculationDataList!![i].component2()
            }
            this.dataMean = tempSum / (calculationDataList!!.size.toDouble())

            tempSum = 0.0
            for (i in calculationDataList!!.indices) {
                tempValue = (calculationDataList!![i].component2()-this.dataMean)
                tempSum += tempValue*tempValue
            }
            this.dataSD = sqrt(tempSum / (calculationDataList!!.size.toDouble()))
        } else {
            this.dataMean = 0.0
            this.dataSD = 0.0

        }



    }

    private fun calculateMedianMaxMin() {
        if (!calculationDataList.isNullOrEmpty()) {

            // Algorithm from http://rosettacode.org/wiki/Averages/Median
            val sortData = calculationDataList!!.toMutableList()
            val elementNumbers: Int = calculationDataList!!.size
            sortData.sortBy { it[1] }
            this.dataMedian =
                (sortData[elementNumbers / 2][1] + sortData[(elementNumbers - 1) / 2][1]) / 2
            this.dataMaximum = sortData[elementNumbers - 1][1]
            this.dataMinimum = sortData[0][1]
        } else {
            this.dataMaximum=0.0
            this.dataMinimum=0.0
            this.dataMedian=0.0
        }
    }
    private fun calculateMode(){
        if (!calculationDataList.isNullOrEmpty()) {
            val tempArray = Array(this.calculationDataList!!.size) { this.calculationDataList!![it][1] }
            this.dataMode= modeOf(tempArray)
        } else {
            this.dataMode = 0.0
        }
    }

}

class dataGenerator() {
    fun generateCyclicData(numPoints: Int, pointsPerCycle: Int): Array<Array<Double>> {
        fun functionAlpha(input: Double, offset: Double, scale1: Double, scale2: Double): Double {
            var alphaResult = offset
            alphaResult += scale1 * cos(input * 2 * PI)
            alphaResult += scale2 * sin(Random.nextDouble(0.0, PI))
            return alphaResult
        }
        //Sabaton("About to Sweep Data")
        var sweepData = Array(numPoints) {
            arrayOf(
                it.toDouble() / pointsPerCycle.toDouble(),
                functionAlpha(it.toDouble(), 25.0, 3.0, 1.0)
            )
        }
        //Sabaton("Data was Sweeped and is being Returned")
        return sweepData
    }

}

fun <T> modeOf(a: Array<T>): T {
    val sortedByFreq = a.groupBy { it }.entries.sortedByDescending { it.value.size }
    val maxFreq = sortedByFreq.first().value.size
    val modes = sortedByFreq.takeWhile { it.value.size == maxFreq }
    return modes.first().key

}


// https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/kotlin/services/dynamodb/src/main/kotlin/com/kotlin/dynamodb/DynamoDBScanItems.kt



/**
To run this Kotlin code example, ensure that you have setup your development environment,
including your credentials.
For information, see this documentation topic:
https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/setup.html
 */


class ModelDatabase(model: DataHandlerViewModel) {
    var viewModel: DataHandlerViewModel = model
    var timeSampleValueToStartScan: Long = 0L

    val staticCredentials = StaticCredentialsProvider {
        accessKeyId = "AKIASIBR7N3FTLI6W5GN"
        secretAccessKey = "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"
    }
    val ddb = DynamoDbClient{
        region = "us-east-1"
        credentialsProvider = staticCredentials
    }



    suspend fun scanItems(ddb: DynamoDbClient, tableNameVal: String) {
        var TVOC_double = 0.0
        var hum_double = 0.0
        var bar_double = 0.0
        var temp_double = 0.0
        var cO2_double = 0.0
        var light_double = 0.0
        var time = 0L
        var timeCheck= false
        var dataCheck= false
        var deviceCheck= false





        var request = ScanRequest {
            tableName = tableNameVal
        }


        val myMap = HashMap<String, String>()
        myMap.put("#keyOfTime", "sample_time")
        val myExMap = mutableMapOf<String, AttributeValue>()
        myExMap.put(":val", AttributeValue.N("$timeSampleValueToStartScan"))


        request = ScanRequest {
            expressionAttributeNames = myMap
            expressionAttributeValues = myExMap
            tableName = tableNameVal
            filterExpression = "#keyOfTime > :val"
        }


        val response = ddb.scan(request)
        ServerTrackerFilter("Starting Server request")
        response.items?.forEach { item ->
            item.keys.forEach { key ->
                ServerTrackerFilter("The key $key has value ${item[key]}")
                when (key) {
                    "device_id"->{
                        deviceCheck = true
                    }
                    "sample_time" -> {
                        timeCheck = true
                        time = item[key].toString().filter { it.isDigit() || it == '.' }.toLong()
                        if(time>timeSampleValueToStartScan){timeSampleValueToStartScan=time}
                        println(time)
                    }
                    "device_data" -> {
                        dataCheck = true
                        val TCOV =
                            splitMyStringTCOV(item[key].toString()).filter { it.isDigit() || it == '.' }
                        val humidity =
                            splitMyStringhumidity(item[key].toString()).filter { it.isDigit() || it == '.' }
                        val barometer =
                            splitMyStringbarometer(item[key].toString()).filter { it.isDigit() || it == '.' }
                        val temp =
                            splitMyStringTemp(item[key].toString()).filter { it.isDigit() || it == '.' }
                        val light =
                            splitMyStringLight(item[key].toString()).filter { it.isDigit() || it == '.' }
                        val cO2 =
                            splitMyStringCO2(item[key].toString()).filter { it.isDigit() || it == '.' }
                        if (TCOV.isEmpty()) {
                            TVOC_double = 0.0
                        } else {
                            TVOC_double = TCOV.toDouble()
                        }
                        if (humidity.isEmpty()) {
                            hum_double = 0.0
                        } else {
                            hum_double = humidity.toDouble()
                        }
                        if (barometer.isEmpty()) {
                            bar_double = 0.0
                        } else {
                            bar_double = barometer.toDouble()
                        }
                        if (temp.isEmpty()) {
                            temp_double = 0.0
                        } else {
                            temp_double = temp.toDouble()
                        }
                        if (light.isEmpty()) {
                            light_double = 0.0
                        } else {
                            light_double = light.toDouble()
                        }
                        if (cO2.isEmpty()) {
                            cO2_double = 0.0
                        } else {
                            cO2_double = cO2.toDouble()
                        }
                        println(TVOC_double)
                        println(hum_double)
                        println(bar_double)
                        println(temp_double)
                        println(light_double)
                        println(cO2_double)
                    }
                }
                if (dataCheck and timeCheck and deviceCheck){
                    dataCheck = false
                    timeCheck = false
                    deviceCheck = false
                    viewModel.temperatureData.loadData(arrayOf(arrayOf(time.toDouble(),temp_double)))
                    viewModel.humidityData.loadData(arrayOf(arrayOf(time.toDouble(),hum_double)))
                    viewModel.tvocData.loadData(arrayOf(arrayOf(time.toDouble(),TVOC_double)))
                    viewModel.pressureData.loadData(arrayOf(arrayOf(time.toDouble(),bar_double)))
                    viewModel.lightData.loadData(arrayOf(arrayOf(time.toDouble(),light_double)))
                    viewModel.co2Data.loadData(arrayOf(arrayOf(time.toDouble(),cO2_double)))
                }
            }
        }
        ServerTrackerFilter("Finish Server request")

    }
    fun splitMyStringTCOV(str: String): String {
        val del1 = "TVOC=N(value="
        val del2 = "), humid"
        val parts = str.split(del1, del2)
        val TCOV = parts[1]
        return TCOV
    }
    fun splitMyStringhumidity(str: String): String {
        val del1 = "humidity=N(value="
        val del2 = "), barometer"
        val parts = str.split(del1, del2)
        val humidity = parts[1]
        return humidity
    }
    fun splitMyStringbarometer(str: String): String {
        val del1 = "barometer=N(value="
        val del2 = "), temp"
        val parts = str.split(del1, del2)
        val barometer = parts[1]
        return barometer
    }
    fun splitMyStringTemp(str: String): String {
        val del1 = "temp=N(value="
        val del2 = "), light"
        val parts = str.split(del1, del2)
        val temp = parts[1]
        return temp
    }
    fun splitMyStringLight(str: String): String {
        val del1 = "light=N(value="
        val del2 = "), CO2"
        val parts = str.split(del1, del2)
        val light = parts[1]
        return light
    }
    fun splitMyStringCO2(str: String): String {
        val del1 = "CO2=N(value="
        val del2 = ")}"
        val parts = str.split(del1, del2)
        val cO2 = parts[1]
        return cO2
    }

    fun scanData() = runBlocking{
        ServerTrackerFilter("Executing scanData")
        scanItems(ddb,"app_data")
    }
}