package ECE558.historicalenviromenttracker

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import ECE558.historicalenviromenttracker.databinding.FragmentPerSensorChartBinding
import com.github.aachartmodel.aainfographics.aachartcreator.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class PerSensorChartFragment : Fragment() {



    //TODO Use this for Graphing https://medium.com/@yilmazvolkan/kotlinlinecharts-c2a730226ff1
    // TODO Make Summary version of each of the Data Type Displays
    // Make it so each line on the graph corresponds to a data type and can be included or expcluded by hilighting the Data Type Display summary





    private var _binding: FragmentPerSensorChartBinding? = null

    private val binding get() = _binding!!

    private val viewModel = DataHandlerPasser.dataHandlerViewModel
    //private val viewModel by viewModels<DataHandlerViewModel>()


    //TODO A single copy of DataHandlerViewModel needs to actually be initialized within
    // MainActivity and I need someway of passing it here

    private lateinit var aaChartModel : AAChartModel

    override fun onCreate(savedInstanceState: Bundle?) {
        StateTrackerFilter("Executing Chart Fragment OnCreate")

        super.onCreate(savedInstanceState)

    }


    //TODO Use the following to figure out how pass data between fragments works
    //https://developer.android.com/guide/navigation/navigation-pass-data#kts
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        StateTrackerFilter("Executing Chart Fragment OnCreateView")

        _binding = FragmentPerSensorChartBinding.inflate(inflater, container, false)


        return binding.root



    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        StateTrackerFilter("Executing Chart Fragment OnViewCreated")

        super.onViewCreated(view, savedInstanceState)
        viewModel.checkServerAndProcessData()  // Does Not erase previous data

        aaChartModel = viewModel.dataChart
        binding.aaChartView.aa_drawChartWithChartModel(aaChartModel)


        binding.buttonFirst.setOnClickListener {
            /*
            if(binding.StartDataRangeView.text.toString().trim().isNotEmpty() and binding.StartDataRangeView.text.toString().trim().isNotBlank()) {
                val passer= binding.StartDataRangeView.text.toString().trim().toLongOrNull()
                if (passer != null) {
                    TimeInputOutputErrorFilter("Taking in new Graph Min $passer")
                    viewModel.setGraphMinTime(passer)
                }else {
                    TimeInputOutputErrorFilter("Graph Min Not Updated because Bad String")

                }
            }else {
                TimeInputOutputErrorFilter("Graph Min Not Updated")

            }
            if(binding.EndDataRangeView.text.toString().trim().isNotEmpty() and binding.EndDataRangeView.text.toString().trim().isNotBlank()) {
                val passer= binding.EndDataRangeView.text.toString().trim().toLongOrNull()
                if (passer != null) {
                    TimeInputOutputErrorFilter("Taking in new Graph Max $passer")
                    viewModel.setGraphMaxTime(passer)
                }else {
                    TimeInputOutputErrorFilter("Graph Min Not Updated because Bad String")

                }
            } else {
                TimeInputOutputErrorFilter("Graph Max Not Updated")

            }
            */


            val start_io = binding.startRangeIo
            val end_io = binding.endRangeIo

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
                viewModel.setGraphMinTime(time1_unix)
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
                viewModel.setGraphMaxTime(time2_unix)

            }

            findNavController().navigate(R.id.action_PerSensorChartFragment_to_PerSensorControllerFragment)
        }

        loadDataToLayout()
    }

    override fun onDestroyView() {
        StateTrackerFilter("Executing Chart Fragment OnDestroyedView")

        super.onDestroyView()
        _binding = null
    }



    fun loadDataToLayout(){
        val simpleDate = SimpleDateFormat("MM/dd/yyyy hh:mm:ss a")

        var t1 = ""
        var t2 = ""
        try {
            t1 = simpleDate.format(Date(viewModel.graphMinimumTime))
        }catch(e: Exception){
            t1= e.toString()
            TimeInputOutputErrorFilter(" Attempted to display ${viewModel.graphMinimumTime}")
            TimeInputOutputErrorFilter(" But got Error ${t1}")
        }
        binding.StartDataRangeView.hint=t1
        try {
            t2 = simpleDate.format(Date(viewModel.graphMaximumTime))
        }catch(e: Exception){
            t2= e.toString()
            TimeInputOutputErrorFilter(" Attempted to display ${viewModel.graphMaximumTime}")
            TimeInputOutputErrorFilter(" But got Error ${t2}")
        }
        binding.EndDataRangeView.hint=t2
        val day_Format = SimpleDateFormat("dd")
        val month_Format = SimpleDateFormat("MM")
        val year_Format = SimpleDateFormat("yyyy")
        val hour_Format = SimpleDateFormat("HH")
        val minute_Format = SimpleDateFormat("mm")
        val second_Format = SimpleDateFormat("ss")

        try {
            binding.startRangeIo.dayIo.setText(day_Format.format(Date(viewModel.graphMinimumTime)))
            binding.startRangeIo.monthIo.setText(month_Format.format(Date(viewModel.graphMinimumTime)))
            binding.startRangeIo.yearIo.setText(year_Format.format(Date(viewModel.graphMinimumTime)))
            binding.startRangeIo.hourIo.setText(hour_Format.format(Date(viewModel.graphMinimumTime)))
            binding.startRangeIo.minuteIo.setText(minute_Format.format(Date(viewModel.graphMinimumTime)))
            binding.startRangeIo.secondIo.setText(second_Format.format(Date(viewModel.graphMinimumTime)))
            binding.endRangeIo.dayIo.setText(day_Format.format(Date(viewModel.graphMaximumTime)))
            binding.endRangeIo.monthIo.setText(month_Format.format(Date(viewModel.graphMaximumTime)))
            binding.endRangeIo.yearIo.setText(year_Format.format(Date(viewModel.graphMaximumTime)))
            binding.endRangeIo.hourIo.setText(hour_Format.format(Date(viewModel.graphMaximumTime)))
            binding.endRangeIo.minuteIo.setText(minute_Format.format(Date(viewModel.graphMaximumTime)))
            binding.endRangeIo.secondIo.setText(second_Format.format(Date(viewModel.graphMaximumTime)))

        } catch(e:Exception){}
        }
        //translateDateTime.toString()
}
