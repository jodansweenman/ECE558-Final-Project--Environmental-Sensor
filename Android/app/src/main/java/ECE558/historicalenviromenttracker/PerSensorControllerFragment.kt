package ECE558.historicalenviromenttracker

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import ECE558.historicalenviromenttracker.databinding.FragmentPerSensorControllerBinding
import ECE558.historicalenviromenttracker.databinding.SensorDataPerTypeLayoutBinding
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId

import java.util.*

import kotlin.math.round

/**
 * A simple [Fragment] subclass as the 2nd destination in the navigation.
 */

//TODO Write Time Range Selection functions


class PerSensorControllerFragment : Fragment() {

    companion object {
        fun newInstance() = PerSensorControllerFragment()
    }
    private var _binding: FragmentPerSensorControllerBinding? = null
    var arrayOfBoundLayOuts:  Array<SensorDataPerTypeLayoutBinding>? = null
    private val binding get() = _binding!!
    private val viewModel = DataHandlerPasser.dataHandlerViewModel
    //private val viewModel by viewModels<DataHandlerViewModel>()
    //TODO A single copy of DataHandlerViewModel needs to actually be initialized within  MainActivity and I need someway of passing it here

    //TODO Need to be able to control sample rate from this screen




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        StateTrackerFilter("Executing Controller Fragment OnCreateView")

        _binding = FragmentPerSensorControllerBinding.inflate(inflater, container, false)
        arrayOfBoundLayOuts = arrayOf( binding.TemperatureData,  binding.HumidityData, binding.VOTCData, binding.PressureData, binding.LightData, binding.CO2Data)

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        StateTrackerFilter("Executing Controller Fragment OnViewCreated")
        viewModel.checkServerAndProcessData()  // Does Not erase previous data
        super.onViewCreated(view, savedInstanceState)
        binding.TemperatureData.DataTypeView.text="Temperature"
        binding.HumidityData.DataTypeView.text="Humidity"
        binding.LightData.DataTypeView.text="Light"
        binding.AltitudeData.DataTypeView.text="Altitude"
        binding.PressureData.DataTypeView.text="Pressure"
        binding.CO2Data.DataTypeView.text="CO2"
        binding.VOTCData.DataTypeView.text="VOTC"


        loadDataToLayout()

        binding.buttonChart.setOnClickListener {
            //viewModel.temperatureData.calculationStartTime= binding.TemperatureData.StartDataRangeView.text.toString().trim().toLong()
            //viewModel.temperatureData.calculationStopTime= binding.TemperatureData.EndDataRangeView.text.toString().trim().toLong()

            for (i in arrayOfBoundLayOuts!!.indices) {
                /*
                if(arrayOfBoundLayOuts!![i].StartDataRangeView.text.toString().trim().isNotEmpty() and arrayOfBoundLayOuts!![i].StartDataRangeView.text.toString().trim().isNotBlank()) {
                    val passer= arrayOfBoundLayOuts!![i].StartDataRangeView.text.toString().trim().toLongOrNull()
                    if (passer != null) { viewModel.dataSetArray[i].calculationStartTime = passer }

                }
                if(arrayOfBoundLayOuts!![i].EndDataRangeView.text.toString().trim().isNotEmpty() and arrayOfBoundLayOuts!![i].EndDataRangeView.text.toString().trim().isNotBlank()) {
                    val passer= arrayOfBoundLayOuts!![i].EndDataRangeView.text.toString().trim().toLongOrNull()
                    if (passer != null) { viewModel.dataSetArray[i].calculationStopTime = passer }
                }

                 */


                val start_io = arrayOfBoundLayOuts!![i].startRangeIo
                val end_io = arrayOfBoundLayOuts!![i].endRangeIo

                val start_io_strings = arrayOf( start_io.hourIo.text.toString(),
                    start_io.minuteIo.text.toString(),  start_io.secondIo.text.toString(),
                    start_io.monthIo.text.toString(), start_io.dayIo.text.toString(), start_io.yearIo.text.toString())
                var start_io_value_list = arrayOf(0,0,0,0,0,0)


                val end_io_strings = arrayOf( end_io.hourIo.text.toString(),
                    end_io.minuteIo.text.toString(),  end_io.secondIo.text.toString(),
                    end_io.monthIo.text.toString(), end_io.dayIo.text.toString(), end_io.yearIo.text.toString())
                var end_io_value_list = arrayOf(0,0,0,0,0,0)
                var time1_valid = true
                var time2_valid = true

                for (j in 0..5) {
                    if (start_io_strings[j].isNotBlank() and start_io_strings[j].isNotEmpty() and (start_io_strings[j].toIntOrNull()!=null)) {
                        start_io_value_list[j] = start_io_strings[j].toInt()
                    }else{
                        time1_valid = false
                    }
                }

                for (j in 0..5) {
                    if (end_io_strings[j].isNotBlank() and end_io_strings[j].isNotEmpty() and (end_io_strings[j].toIntOrNull()!=null)) {
                        end_io_value_list[j] = end_io_strings[j].toInt()
                    } else{
                        time2_valid = false
                    }
                }
                if(time1_valid) {
                    val time1 = LocalDateTime.of(
                        start_io_value_list[5],
                        start_io_value_list[3],
                        start_io_value_list[4],
                        start_io_value_list[0],
                        start_io_value_list[1],
                        start_io_value_list[2]
                    )
                    val time1_unix = time1.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    viewModel.dataSetArray[i].calculationStartTime = time1_unix
                }
                if(time2_valid) {
                    val time2 = LocalDateTime.of(
                        end_io_value_list[5],
                        end_io_value_list[3],
                        end_io_value_list[4],
                        end_io_value_list[0],
                        end_io_value_list[1],
                        end_io_value_list[2]
                    )
                    val time2_unix = time2.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    viewModel.dataSetArray[i].calculationStopTime = time2_unix

                }




            }
            findNavController().navigate(R.id.action_PerSensorControllerFragment_to_PerSensorChartFragment)
        }

        binding.TemperatureData.StartDataRangeView.setOnClickListener{}

    }

    override fun onDestroyView() {
        StateTrackerFilter("Executing Controller Fragment onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    fun loadDataToLayout(){
        fun round1000ths(number: Double): Double {
            return round(number * 1000) / 1000.0
        }
        if(!arrayOfBoundLayOuts.isNullOrEmpty()) {
            val simpleDate = SimpleDateFormat("MM/dd/yyyy hh:mm:ss a")
            var t1 = ""
            var t2 = ""


            for (i in arrayOfBoundLayOuts!!.indices) {
                arrayOfBoundLayOuts!![i].MeanValueView.text =
                    round1000ths(viewModel.dataSetArray[i].dataMean).toString()
                arrayOfBoundLayOuts!![i].MaximumValueView.text =
                    round1000ths(viewModel.dataSetArray[i].dataMaximum).toString()
                arrayOfBoundLayOuts!![i].MinimumValueView.text =
                    round1000ths(viewModel.dataSetArray[i].dataMinimum).toString()
                arrayOfBoundLayOuts!![i].MedianValueView.text =
                    round1000ths(viewModel.dataSetArray[i].dataMedian).toString()
                arrayOfBoundLayOuts!![i].ModeValueView.text =
                    round1000ths(viewModel.dataSetArray[i].dataMode).toString()
                arrayOfBoundLayOuts!![i].StandardDeviationValueView.text =
                    round1000ths(viewModel.dataSetArray[i].dataSD).toString()
                arrayOfBoundLayOuts!![i].SampleNumberView.text =
                    viewModel.dataSetArray[i].calculationDataList!!.size.toString()

                try {
                    t1 = simpleDate.format(Date(viewModel.dataSetArray[i].calculationStartTime))
                }catch(e: Exception){
                    t1= e.toString()
                }
                try {
                    t2 = simpleDate.format(Date(viewModel.dataSetArray[i].calculationStopTime))
                }catch(e: Exception){
                    t2= e.toString()
                }
                arrayOfBoundLayOuts!![i].StartDataRangeView.hint=t1
                arrayOfBoundLayOuts!![i].EndDataRangeView.hint=t2

                val day_Format = SimpleDateFormat("dd")
                val month_Format = SimpleDateFormat("MM")
                val year_Format = SimpleDateFormat("yyyy")
                val hour_Format = SimpleDateFormat("HH")
                val minute_Format = SimpleDateFormat("mm")
                val second_Format = SimpleDateFormat("ss")

                try {
                    arrayOfBoundLayOuts!![i].startRangeIo.dayIo.setText(day_Format.format(Date(viewModel.dataSetArray[i].calculationStartTime)))
                    arrayOfBoundLayOuts!![i].startRangeIo.monthIo.setText(month_Format.format(Date(viewModel.dataSetArray[i].calculationStartTime)))
                    arrayOfBoundLayOuts!![i].startRangeIo.yearIo.setText(year_Format.format(Date(viewModel.dataSetArray[i].calculationStartTime)))
                    arrayOfBoundLayOuts!![i].startRangeIo.hourIo.setText(hour_Format.format(Date(viewModel.dataSetArray[i].calculationStartTime)))
                    arrayOfBoundLayOuts!![i].startRangeIo.minuteIo.setText(minute_Format.format(Date(viewModel.dataSetArray[i].calculationStartTime)))
                    arrayOfBoundLayOuts!![i].startRangeIo.secondIo.setText(second_Format.format(Date(viewModel.dataSetArray[i].calculationStartTime)))
                    arrayOfBoundLayOuts!![i].endRangeIo.dayIo.setText(day_Format.format(Date(viewModel.dataSetArray[i].calculationStopTime)))
                    arrayOfBoundLayOuts!![i].endRangeIo.monthIo.setText(month_Format.format(Date(viewModel.dataSetArray[i].calculationStopTime)))
                    arrayOfBoundLayOuts!![i].endRangeIo.yearIo.setText(year_Format.format(Date(viewModel.dataSetArray[i].calculationStopTime)))
                    arrayOfBoundLayOuts!![i].endRangeIo.hourIo.setText(hour_Format.format(Date(viewModel.dataSetArray[i].calculationStopTime)))
                    arrayOfBoundLayOuts!![i].endRangeIo.minuteIo.setText(minute_Format.format(Date(viewModel.dataSetArray[i].calculationStopTime)))
                    arrayOfBoundLayOuts!![i].endRangeIo.secondIo.setText(second_Format.format(Date(viewModel.dataSetArray[i].calculationStopTime)))

                } catch(e:Exception){}

            }
        }

    }
}
