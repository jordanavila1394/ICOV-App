package com.icov.app.activities

import android.Manifest
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.icov.app.R
import com.icov.app.config.AppConfig
import com.icov.app.models.UserMongoDb
import com.icov.app.databinding.ActivityMainBinding
import com.icov.app.utils.CommonFunctions
import com.icov.app.utils.GetCurrentLocation
import com.icov.app.utils.GetTimeUtil
import com.icov.app.utils.Listener
import hotchemi.android.rate.AppRate
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.User
import io.realm.mongodb.mongo.MongoClient
import io.realm.mongodb.mongo.MongoCollection
import io.realm.mongodb.mongo.MongoDatabase
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bson.Document

class MainActivity : AppCompatActivity() {

    private lateinit var bind: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var app: App
    private lateinit var user: User
    private lateinit var mongoClient: MongoClient
    private lateinit var mongoDatabase: MongoDatabase
    private lateinit var mongoUsersCollection: MongoCollection<Document>
    private lateinit var mongoAttendanceCollection: MongoCollection<Document>

    private lateinit var loadingDialog: Dialog
    private lateinit var rateUsDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)
        initializeVariables()
        setupTheme()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        if (bind.navView.getHeaderView(0).full_name_drawer.text.toString() != UserMongoDb.fullName) {
            bind.navView.getHeaderView(0).full_name_drawer.text = UserMongoDb.fullName
        }
    }

    private fun initializeVariables() {
        app = App(AppConfiguration.Builder(AppConfig.REALM_APP_ID).build())
        user = app.currentUser()!!
        mongoClient = user.getMongoClient("mongodb-atlas")
        mongoDatabase = mongoClient.getDatabase("icovDB")
        mongoUsersCollection = mongoDatabase.getCollection("users")
        mongoAttendanceCollection = mongoDatabase.getCollection("attendance")
        loadingDialog = CommonFunctions.createDialog(
            this,
            R.layout.loading_progress_dialog,
            R.drawable.progress_circle,
            false
        )
        rateUsDialog = CommonFunctions.createDialog(
            this,
            R.layout.rate_us_dialog,
            R.drawable.slider_background,
            true
        )
    }

    private fun setupTheme() {
        setSupportActionBar(bind.include.toolbar)
        setupNavigationDrawer()

        // app rate
        AppRate
            .with(this)
            .setInstallDays(1)
            .setLaunchTimes(3)
            .setRemindInterval(3)
            .monitor()

        AppRate.showRateDialogIfMeetsConditions(this)
        // app rate
        setupUserData()
    }

    private fun setupUserData() {
        loadingDialog.show()

        val queryFilter = Document("user_id", user.id)
        mongoUsersCollection.findOne(queryFilter)?.getAsync { result ->
            if (result.isSuccess) {
                val firstNameValue = result.get().getString("name")
                val surnameValue = result.get().getString("surname")

                UserMongoDb.firstName = firstNameValue
                UserMongoDb.surname = surnameValue
                UserMongoDb.fullName = "$firstNameValue $surnameValue"
                UserMongoDb.email = result.get().getString("email")
                UserMongoDb.password = result.get().getString("password")

                bind.navView.getHeaderView(0).full_name_drawer.text = UserMongoDb.fullName
                bind.navView.getHeaderView(0).email_id_drawer.text = UserMongoDb.email

                getAttendanceData()

            } else {
                loadingDialog.dismiss()
                Log.d("Error", "Failed to find document with: ${result.error}")
            }
        }
    }

    private fun getAttendanceData() {
        val queryFilter = Document("user_id", user.id)
        var uniqueDocID: String? = null

        val timeListener = object : Listener {
            override fun onTimeReceived(
                time: String?,
                amOrPm: String?,
                dayOfWeek: String?,
                date: String?
            ) {
                Log.d("time", "received: $time, $amOrPm, $dayOfWeek, $date")

                val nowDate =  "$dayOfWeek, $date"

                GlobalScope.launch(Dispatchers.Main) {
                    mongoAttendanceCollection.find(queryFilter).iterator().getAsync { result ->
                        if (result.isSuccess) {
                            Log.d("Attendance", "successfully found all documents for the user!")

                            result.get().forEach {
                                if (it.getString("check_in_date") == nowDate) {
                                    uniqueDocID = it.getString("doc_id")
                                }
                            }

                            if (uniqueDocID != null) {
                                val docQueryFilter = Document("doc_id", uniqueDocID)
                                mongoAttendanceCollection.findOne(docQueryFilter).getAsync { result1 ->
                                    if (result1.isSuccess) {
                                        UserMongoDb.checkedIn = true
                                        UserMongoDb.checkedInTimeToday = result1.get().getString("check_in_time")
                                        UserMongoDb.checkedInDateToday = result1.get().getString("check_in_date")
                                        UserMongoDb.checkedInLatitude = result1.get().getDouble("checked_in_latitude")
                                        UserMongoDb.checkedInLongitude = result1.get().getDouble("checked_in_longitude")

                                        if (result1.get().getString("check_out_date") != "Not Yet") {
                                            UserMongoDb.checkedOut = true
                                            UserMongoDb.checkedOutTimeToday = result1.get().getString("check_out_time")
                                            UserMongoDb.checkedOutDateToday = result1.get().getString("check_out_date")
                                            UserMongoDb.checkedOutLatitude = result1.get().getDouble("checked_out_latitude")
                                            UserMongoDb.checkedOutLongitude = result1.get().getDouble("checked_out_longitude")
                                        }
                                        loadingDialog.dismiss()

                                    } else {
                                        loadingDialog.dismiss()
                                        Log.d("Error", "Failed to find document with: ${result1.error}")
                                    }
                                }
                            } else {
                                loadingDialog.dismiss()
                            }

                        } else {
                            loadingDialog.dismiss()
                            Log.d("Error", "Failed to find document with: ${result.error}")
                        }
                    }
                }

            }

            override fun onError(ex: Exception?) {
                Log.d("error", "error ${ex.toString()}")
            }
        }

        GetTimeUtil.getDate(timeListener)

    }


    private fun setupNavigationDrawer() {
        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_my_account,
                R.id.nav_attendance,
                R.id.nav_settings,
                R.id.nav_rate_us,
                R.id.nav_share,
                R.id.nav_log_out
            ), bind.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        bind.navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.updateUserInfoFragment) {
                supportActionBar?.hide()
            } else {
                if (!supportActionBar!!.isShowing) {
                    supportActionBar?.show()
                }
            }
        }

        bind.navView.menu.findItem(R.id.nav_rate_us).setOnMenuItemClickListener {
            bind.drawerLayout.closeDrawer(GravityCompat.START)
            rateUsDialog.show()
            true
        }

        bind.navView.menu.findItem(R.id.nav_share).setOnMenuItemClickListener {
            bind.drawerLayout.closeDrawer(GravityCompat.START)
            shareAppFun()
            true
        }

        bind.navView.menu.findItem(R.id.nav_log_out).setOnMenuItemClickListener {
            bind.drawerLayout.closeDrawer(GravityCompat.START)
            logOut()
            true
        }

    }

    private fun setupClickListeners() {
        rateUsDialog.findViewById<TextView>(R.id.rate_now_btn).setOnClickListener {
            rateAppFun()
        }
        rateUsDialog.findViewById<TextView>(R.id.no_thanks_btn).setOnClickListener {
            rateUsDialog.dismiss()
        }
        rateUsDialog.findViewById<TextView>(R.id.remind_me_later_btn).setOnClickListener {
            rateUsDialog.dismiss()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun rateAppFun() {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=${packageName}")
                )
            )
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=${packageName}")
                )
            )
        }
        rateUsDialog.dismiss()
    }

    private fun shareAppFun() {
        try {
            val shareAppIntent = Intent(Intent.ACTION_SEND)
            shareAppIntent.type = "text/plain"
            shareAppIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                String.format(getString(R.string.share_intent_msg), getString(R.string.app_name))
            )
            val shareMessage = "http://play.google.com/store/apps/details?id=$packageName"
            shareAppIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            startActivity(Intent.createChooser(shareAppIntent, getString(R.string.app_name)))
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logOut() {
        loadingDialog.show()
        app.currentUser()?.logOutAsync { result ->
            if (result.isSuccess) {
                loadingDialog.dismiss()
                CommonFunctions.startIntent(this, RegisterActivity::class.java, true)
            } else {
                loadingDialog.dismiss()
                Toast.makeText(
                    this,
                    "Failed to log out, error: ${result.error}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onBackPressed() {
        if (bind.navView.checkedItem?.itemId == R.id.nav_home) {
            val clickListener: DialogInterface.OnClickListener =
                DialogInterface.OnClickListener { _, _ ->
                    finishAndRemoveTask()
                    super.onBackPressed()
                }

            AlertDialog.Builder(this@MainActivity)
                .setTitle(getString(R.string.are_you_sure))
                .setMessage(
                    String.format(
                        getString(R.string.process_delete),
                        getString(R.string.app_name)
                    )
                )
                .setPositiveButton(getString(R.string.ok), clickListener)
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
            return
        }
        super.onBackPressed()

    }

}