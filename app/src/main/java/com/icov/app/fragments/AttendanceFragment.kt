package com.icov.app.fragments

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.icov.app.R
import com.icov.app.config.AppConfig
import com.icov.app.databinding.FragmentAttendanceBinding
import com.icov.app.models.UserMongoDb
import com.icov.app.utils.CommonFunctions
import com.icov.app.utils.GetCurrentLocation
import com.icov.app.utils.GetTimeUtil
import com.icov.app.utils.Listener
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.AppException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bson.Document
import java.util.*


class AttendanceFragment : Fragment() {

    private var _binding: FragmentAttendanceBinding? = null
    private val binding get() = _binding!!
    private lateinit var app: App

    private lateinit var navController: NavController
    private lateinit var timeListener: Listener
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    private val REQUEST_CODE_LOCATION = 101

    private lateinit var loadingDialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        setupTheme()
        setupClickListeners()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(runnable, 30000)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupTheme() {
        app = App(AppConfiguration.Builder(AppConfig.REALM_APP_ID).build())
        loadingDialog = CommonFunctions.createDialog(
            requireContext(),
            R.layout.loading_progress_dialog,
            R.drawable.progress_circle,
            false
        )
        loadingDialog.show()
        handler = Handler(Looper.getMainLooper())
        timeListener = object : Listener {
            override fun onTimeReceived(
                time: String?,
                amOrPm: String?,
                dayOfWeek: String?,
                date: String?
            ) {
                Log.d("time", "received: $time, $amOrPm, $dayOfWeek, $date")
                GlobalScope.launch(Dispatchers.Main) {
                    binding.currentTime.text = time
                    binding.amOrPm.text = amOrPm
                    binding.currentDay.text = dayOfWeek
                    binding.currentDate.text = date
                    loadingDialog.dismiss()
                }
            }

            override fun onError(ex: Exception?) {
                Log.d("error", "error ${ex.toString()}")
            }
        }
        runnable = Runnable {
            kotlin.run {
                GetTimeUtil.getDate(timeListener)
                handler.postDelayed(runnable, 30000)
            }
        }
        handler.post(runnable)

        if (UserMongoDb.checkedIn) {
            if (UserMongoDb.checkedOut) {
                binding.clockOutBtn.isEnabled = false
                binding.clockOutTime.visibility = View.VISIBLE
                binding.clockOutTime.text = String.format(getString(R.string.clock_out_time), UserMongoDb.checkedOutTimeToday)

            } else {
                binding.clockOutBtn.isEnabled = true
                binding.clockOutTime.visibility = View.GONE
            }
            binding.clockInBtn.visibility = View.GONE
            binding.clockInBtn.isEnabled = false

            binding.clockOutBtn.visibility = View.VISIBLE
            binding.clockInTime.visibility = View.VISIBLE
            binding.clockInTime.text = String.format(getString(R.string.clock_in_time), UserMongoDb.checkedInTimeToday)

        } else {
            binding.clockInBtn.visibility = View.VISIBLE
            binding.clockInBtn.isEnabled = true

            binding.clockOutBtn.visibility = View.GONE
            binding.clockOutBtn.isEnabled = false
            binding.clockInTime.visibility = View.GONE
            binding.clockOutTime.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.clockInBtn.setOnClickListener {
            clockIn()
        }
        binding.clockOutBtn.setOnClickListener {
            clockOut()
        }
        binding.calendarBtn.setOnClickListener {
            navController.navigate(R.id.action_nav_attendance_to_calendarFragment)
        }
    }

    private fun clockIn() {
        checkPermissionsAndOs(true)
    }

    private fun clockOut() {
        checkPermissionsAndOs(false)
    }

    private fun checkPermissionsAndOs(toClockIn: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (CommonFunctions.hasPermissions(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) &&
                CommonFunctions.hasPermissions(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) &&
                CommonFunctions.hasPermissions(
                    requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            ) {
                createDataOnDatabase(toClockIn)
            } else {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ), REQUEST_CODE_LOCATION
                )
            }
        } else {

            if (CommonFunctions.hasPermissions(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) &&
                CommonFunctions.hasPermissions(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                createDataOnDatabase(toClockIn)
            } else {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ), REQUEST_CODE_LOCATION
                )
            }
        }
    }

    private fun createDataOnDatabase(toClockIn: Boolean) {
        loadingDialog.show()
        val location: Location? = GetCurrentLocation.getLocation(requireContext())

        if (location != null) {
            val user = app.currentUser()!!
            val mongoClient = user.getMongoClient("mongodb-atlas")
            val database = mongoClient.getDatabase("icovDB")
            val mongoCollection = database.getCollection("attendance")

            if (toClockIn) {
                val docID: String = UUID.randomUUID().toString()

                val document = Document("user_id", user.id)
                    .append("doc_id", docID)
                    .append("name", UserMongoDb.firstName)
                    .append("surname", UserMongoDb.surname)
                    .append("check_in_time", "${binding.currentTime.text} ${binding.amOrPm.text}")
                    .append(
                        "check_in_date",
                        "${binding.currentDay.text}, ${binding.currentDate.text}"
                    )
                    .append("checked_in_latitude", location.latitude)
                    .append("checked_in_longitude", location.longitude)
                    .append("check_out_time", "Not Yet")
                    .append("check_out_date", "Not Yet")
                    .append("device_imei", user.deviceId)

                mongoCollection.insertOne(document).getAsync {
                    if (it.isSuccess) {
                        updateUI(true, location)
                    } else {
                        showError(it.error)
                    }
                }

            } else {

                var uniqueDocID: String? = null
                val queryFilter = Document("user_id", user.id)

                mongoCollection.find(queryFilter).iterator().getAsync { result ->
                    if (result.isSuccess) {
                        Log.d("Clock Out", "successfully found all documents for the user!")

                        result.get().forEach {
                            if (it.getString("check_in_date") == UserMongoDb.checkedInDateToday) {
                                uniqueDocID = it.getString("doc_id")
                            }
                        }

                        val docQueryFilter = Document("doc_id", uniqueDocID)
                        val updateDocument = Document("user_id", user.id)
                            .append("doc_id", uniqueDocID)
                            .append("name", UserMongoDb.firstName)
                            .append("surname", UserMongoDb.surname)
                            .append("check_in_time", UserMongoDb.checkedInTimeToday)
                            .append("check_in_date", UserMongoDb.checkedInDateToday)
                            .append("checked_in_latitude", UserMongoDb.checkedInLatitude)
                            .append("checked_in_longitude", UserMongoDb.checkedInLongitude)
                            .append(
                                "check_out_time",
                                "${binding.currentTime.text} ${binding.amOrPm.text}"
                            )
                            .append(
                                "check_out_date",
                                "${binding.currentDay.text}, ${binding.currentDate.text}"
                            )
                            .append("checked_out_latitude", location.latitude)
                            .append("checked_out_longitude", location.longitude)
                            .append("device_imei", user.deviceId)


                        mongoCollection.findOneAndUpdate(docQueryFilter, updateDocument).getAsync {
                            if (it.isSuccess) {
                                updateUI(false, location)
                            } else {
                                showError(result.error)
                            }
                        }
                    } else {
                        showError(result.error)
                    }
                }
            }
        } else {
            showError(null)
        }

    }

    private fun updateUI(toClockIn: Boolean, location: Location) {
        binding.clockInBtn.visibility = View.GONE
        binding.clockInBtn.isEnabled = false
        binding.clockInTime.visibility = View.VISIBLE

        binding.clockOutBtn.visibility = View.VISIBLE

        if (toClockIn) {
            binding.clockOutBtn.isEnabled = true
            binding.clockOutTime.visibility = View.GONE

            UserMongoDb.checkedIn = true
            UserMongoDb.checkedInTimeToday =
                "${binding.currentTime.text} ${binding.amOrPm.text}"
            UserMongoDb.checkedInDateToday =
                "${binding.currentDay.text}, ${binding.currentDate.text}"
            UserMongoDb.checkedInLatitude = location.latitude
            UserMongoDb.checkedInLongitude = location.longitude

            Toast.makeText(
                requireContext(),
                "Clocked in successfully!",
                Toast.LENGTH_SHORT
            ).show()

        } else {
            binding.clockOutBtn.isEnabled = false
            binding.clockOutTime.visibility = View.VISIBLE

            UserMongoDb.checkedOut = true
            UserMongoDb.checkedOutTimeToday =
                "${binding.currentTime.text} ${binding.amOrPm.text}"
            UserMongoDb.checkedOutDateToday =
                "${binding.currentDay.text}, ${binding.currentDate.text}"
            UserMongoDb.checkedOutLatitude = location.latitude
            UserMongoDb.checkedOutLongitude = location.longitude

            binding.clockOutTime.text = String.format(getString(R.string.clock_out_time), UserMongoDb.checkedOutTimeToday)

            Toast.makeText(
                requireContext(),
                "Clocked out successfully!",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.clockInTime.text = String.format(getString(R.string.clock_in_time), UserMongoDb.checkedInTimeToday)
        loadingDialog.dismiss()
    }

    private fun showError(error: AppException?) {
        loadingDialog.dismiss()

        if (error != null) {
            Log.d(
                "error",
                "${error.errorMessage}  ${error.errorCode}"
            )
            error.printStackTrace()
            Toast.makeText(
                requireContext(),
                error.toString(),
                Toast.LENGTH_SHORT
            ).show()

        } else {
            Toast.makeText(
                requireContext(),
                "Failed to get location! Please turn on your gps from settings!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(
                        requireContext(),
                        "Thanks for giving permissions! Now try again!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "We required these permissions!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

}