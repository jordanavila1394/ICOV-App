package com.icov.app.activities

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.icov.app.R
import com.icov.app.config.AppConfig
import com.icov.app.database.UserMongoDb
import com.icov.app.fragments.*
import com.icov.app.utils.Functions
import hotchemi.android.rate.AppRate
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.User
import io.realm.mongodb.mongo.MongoClient
import io.realm.mongodb.mongo.MongoCollection
import io.realm.mongodb.mongo.MongoDatabase
import org.bson.Document


class MainActivity : AppCompatActivity() {
    private val TAG = "MAIN_ACTIVITY"

    private lateinit var app: App
    private lateinit var user: User
    private lateinit var mongoClient: MongoClient
    private lateinit var mongoDatabase: MongoDatabase
    private lateinit var mongoCollection: MongoCollection<Document>

    private var currentFragment = -1
    private val HOME_FRAGMENT = 0
    private val MY_ACCOUNT_FRAGMENT = 1
    private val ATTENDANCE_FRAGMENT = 2
    private val NEWS_FRAGMENT = 3
    private val SETTINGS_FRAGMENT = 4

    private lateinit var toolbar: Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var menuItem: MenuItem

    private lateinit var loadingDialog: Dialog
    private lateinit var rateUsDialog: Dialog

    // nav header
    private lateinit var fullNameDrawer: TextView
    private lateinit var emailDrawer: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeVariables()
        setupTheme()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()

        if (fullNameDrawer.text.toString() != UserMongoDb.fullName) {
            fullNameDrawer.text = UserMongoDb.fullName
        }

    }

    private fun initializeVariables() {
        app = App(AppConfiguration.Builder(AppConfig.REALM_APP_ID).build())
        user = app.currentUser()!!
        mongoClient = user.getMongoClient("mongodb-atlas")
        mongoDatabase = mongoClient.getDatabase("icovDB")
        mongoCollection = mongoDatabase.getCollection("users")

        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        fullNameDrawer = navView.getHeaderView(0).findViewById(R.id.full_name_drawer)
        emailDrawer = navView.getHeaderView(0).findViewById(R.id.email_id_drawer)

        loadingDialog = Functions.createDialog(this, R.layout.loading_progress_dialog, false)
        rateUsDialog = Functions.createDialog(this, R.layout.rate_us_dialog, true)
    }

    private fun setupTheme() {
        setSupportActionBar(toolbar)
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // app rate
        AppRate
            .with(this)
            .setInstallDays(1)
            .setLaunchTimes(3)
            .setRemindInterval(3)
            .monitor()

        AppRate.showRateDialogIfMeetsConditions(this)
        // app rate

        goToFragment(getString(R.string.app_name), HomeFragment(), HOME_FRAGMENT)

        val queryFilter = Document("user_id", user.id)
        mongoCollection
            .findOne(queryFilter)?.getAsync { result ->
                if (result.isSuccess) {
                    Log.d(TAG, "successfully found a document: ${result.get()}")
                    val firstNameValue = result.get().getString("name")
                    val surnameValue = result.get().getString("surname")

                    UserMongoDb.firstName = firstNameValue
                    UserMongoDb.surname = surnameValue
                    UserMongoDb.fullName = "$firstNameValue $surnameValue"
                    UserMongoDb.email = result.get().getString("email")
                    UserMongoDb.password = result.get().getString("password")

                    fullNameDrawer.text = UserMongoDb.fullName
                    emailDrawer.text = UserMongoDb.email
                } else {
                    Log.d(TAG, "Failed to find document with: ${result.error}")
                }
            }
    }

    private fun setupClickListeners() {
        navView.setNavigationItemSelectedListener { item ->
            val drawer: DrawerLayout = findViewById(R.id.drawer_layout)
            drawer.closeDrawer(GravityCompat.START)
            menuItem = item

            drawer.addDrawerListener(object : SimpleDrawerListener() {
                override fun onDrawerClosed(drawerView: View) {
                    super.onDrawerClosed(drawerView)

                    when (menuItem.itemId) {
                        R.id.nav_home -> {
                            invalidateOptionsMenu()
                            goToFragment(
                                getString(R.string.app_name),
                                HomeFragment(),
                                HOME_FRAGMENT
                            )
                        }
                        R.id.nav_my_account -> {
                            goToFragment(
                                getString(R.string.my_account),
                                MyAccountFragment(),
                                MY_ACCOUNT_FRAGMENT
                            )

                        }
                        R.id.nav_attendance -> {
                            goToFragment(
                                getString(R.string.attendance),
                                AttendanceFragment(),
                                ATTENDANCE_FRAGMENT
                            )

                        }
                        R.id.nav_news -> {
                            goToFragment(
                                getString(R.string.news),
                                NewsFragment(),
                                NEWS_FRAGMENT
                            )

                        }
                        R.id.nav_settings -> {
                            goToFragment(
                                getString(R.string.settings),
                                SettingsFragment(),
                                SETTINGS_FRAGMENT
                            )
                        }
                        R.id.nav_rate_us -> {
                            rateUsDialog.show()
                        }
                        R.id.nav_share -> {
                            shareAppFun()
                        }
                        R.id.nav_log_out -> {
                            logOut()
                        }
                    }

                    drawer.removeDrawerListener(this)
                }
            })
            true
        }
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
        app.currentUser()?.logOutAsync() { result ->
            if (result.isSuccess) {
                loadingDialog.dismiss()
                Functions.startIntent(this, RegisterActivity::class.java, true)
                Log.d(TAG, "Successfully logged out.")
            } else {
                loadingDialog.dismiss()
                Toast.makeText(
                    this,
                    "Failed to log out, error: ${result.error}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d(TAG, "Failed to log out, error: ${result.error}")
            }
        }
    }

    override fun onBackPressed() {
        val drawer: DrawerLayout = findViewById(R.id.drawer_layout)

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            if (currentFragment == HOME_FRAGMENT) {
                val clickListener: DialogInterface.OnClickListener =
                    DialogInterface.OnClickListener { dialogInterface, i ->
                        finishAndRemoveTask()
                        super.onBackPressed()
                    }

                val alertDialog: AlertDialog = AlertDialog.Builder(this@MainActivity)
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

            } else {
                invalidateOptionsMenu()
                goToFragment(getString(R.string.app_name), HomeFragment(), HOME_FRAGMENT)
            }
        }
    }

    private fun goToFragment(title: String, fragment: Fragment, fragmentNo: Int) {
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = title
        invalidateOptionsMenu()
        setFragment(fragment, fragmentNo)
        Log.d(TAG, "SET FRAGMENT: ${title}  ${fragmentNo}")

    }

    private fun setFragment(fragment: Fragment, fragmentNo: Int) {
        if (fragmentNo != currentFragment) {
            currentFragment = fragmentNo
            navView.menu.getItem(fragmentNo).isChecked = true

            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.main_frame_layout, fragment)
                .commit()
        }
    }

}