package com.kabbodev.mongodb.fragments

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import com.kabbodev.mongodb.R
import com.kabbodev.mongodb.activities.PrivacyOrTermsActivity
import com.kabbodev.mongodb.utils.Functions

class SettingsFragment : PreferenceFragmentCompat() {

    private val requestUpdateCode = 121
    private val developerEmail = "kabboandreigns@gmail.com"

    private var checkedItemLanguage: Int? = null
    private lateinit var languageSettings: Preference
    private lateinit var checkForUpdates: Preference
    private lateinit var appVersion: Preference
    private lateinit var reportBugs: Preference
    private lateinit var sendFeedback: Preference
    private lateinit var website: Preference
    private lateinit var privacyPolicy: Preference
    private lateinit var termsAndConditions: Preference

    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var appUpdateInfoTask: Task<AppUpdateInfo>

    private lateinit var feedBackDialog: Dialog
    private lateinit var reportBugDialog: Dialog

    private var selectedRating = -1
    private var feedback1star: TextView? = null
    private var feedback2star: TextView? = null
    private var feedback3star: TextView? = null
    private var feedback4star: TextView? = null
    private var feedback5star: TextView? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_pref, rootKey)
        initializeVariables()
        setupTheme()
        setupClickListeners()
    }

    private fun initializeVariables() {
        languageSettings = findPreference("language")!!
        checkForUpdates = findPreference("check_for_updates")!!
        appVersion = findPreference("app_version")!!
        reportBugs = findPreference("report_bug")!!
        sendFeedback = findPreference("send_feedback")!!
        website = findPreference("website")!!
        privacyPolicy = findPreference("privacy_policy")!!
        termsAndConditions = findPreference("terms_and_conditions")!!

        feedBackDialog = Functions.createDialog(requireContext(), R.layout.send_feedback_dialog, true)
        reportBugDialog = Functions.createDialog(requireContext(), R.layout.report_bug_dialog, true)

        feedback1star = feedBackDialog.findViewById(R.id.layout_1_star)
        feedback2star = feedBackDialog.findViewById(R.id.layout_2_star)
        feedback3star = feedBackDialog.findViewById(R.id.layout_3_star)
        feedback4star = feedBackDialog.findViewById(R.id.layout_4_star)
        feedback5star = feedBackDialog.findViewById(R.id.layout_5_star)
    }

    private fun setupTheme() {
        var version = "1.0"
        try {
            val packageInfo = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
            version = packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        appVersion.summary = version

        appUpdateManager = AppUpdateManagerFactory.create(requireContext())
        appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { result ->
            if (result.updateAvailability() === UpdateAvailability.UPDATE_AVAILABLE
                && result.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                // set text for update
                checkForUpdates.summary = getString(R.string.update_available)
                appVersion.summary = "$version ${getString(R.string.update_required)}"
            }
        }

        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        when (pref.getString("language", "en")) {
            "en" -> {
                languageSettings.summary = "English"
                checkedItemLanguage = 0
            }

//            "it" -> {
//                languageSettings.summary = "Italian"
//                checkedItemLanguage = 1
//            }
        }
    }

    private fun setupClickListeners() {
        feedBackDialog.findViewById<Button>(R.id.send_feedback_btn)?.setOnClickListener {
            checkInputs(feedBackDialog, false)
        }
        feedback1star?.setOnClickListener { view ->
            setBackgroundColorOfTV(view as TextView?, 1)
        }
        feedback2star?.setOnClickListener { view ->
            setBackgroundColorOfTV(view as TextView?, 2)
        }
        feedback3star?.setOnClickListener { view ->
            setBackgroundColorOfTV(view as TextView?, 3)
        }
        feedback4star?.setOnClickListener { view ->
            setBackgroundColorOfTV(view as TextView?, 4)
        }
        feedback5star?.setOnClickListener { view ->
            setBackgroundColorOfTV(view as TextView?, 5)
        }
        reportBugDialog.findViewById<Button>(R.id.report_bug_btn)?.setOnClickListener {
            checkInputs(reportBugDialog, true)
        }

        var intent: Intent
//        languageSettings.setOnPreferenceClickListener {
//            val mBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
//            mBuilder.setTitle(getString(R.string.select_language))
//
//            val clickListener = DialogInterface.OnClickListener { dialogInterface, pos ->
//                if (pos != checkedItemLanguage) {
//                    when (pos) {
//                        0 -> {
//                            setLanguage("en")
//                        }
//                        1 -> {
//                            setLanguage("it")
//                        }
//                    }
////                    Functions.startIntent(requireActivity(), SplashActivity::class.java, true)
//                }
//                dialogInterface.dismiss()
//            }
//
//            mBuilder.setSingleChoiceItems(R.array.language_names, checkedItemLanguage!!, clickListener)
//            val alertDialog = mBuilder.create()
//            alertDialog.show()
//
//            false
//        }
        checkForUpdates.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            appUpdateManager = AppUpdateManagerFactory.create(requireContext())
            appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { result: AppUpdateInfo ->
                if (result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && result.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(result, AppUpdateType.IMMEDIATE, requireActivity(), requestUpdateCode)
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                    }
                }
            }
            false
        }
        website.setOnPreferenceClickListener {
            intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.icov.it/"))
            startActivity(intent)
            false
        }
        privacyPolicy.setOnPreferenceClickListener {
            intent = Intent(context, PrivacyOrTermsActivity::class.java)
            intent.putExtra("type", "Privacy")
            startActivity(intent)
            false
        }
        termsAndConditions.setOnPreferenceClickListener {
            intent = Intent(context, PrivacyOrTermsActivity::class.java)
            intent.putExtra("type", "Terms")
            startActivity(intent)
            false
        }
        reportBugs.setOnPreferenceClickListener {
            reportBugDialog.show()
            false
        }
        sendFeedback.setOnPreferenceClickListener {
            feedBackDialog.show()
            false
        }
    }

    private fun setLanguage(language: String) {
        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = preferences.edit()
        editor.putString("language", language)
        editor.apply()
    }

    private fun setBackgroundColorOfTV(selected: TextView?, currentViewID: Int) {
        if (selectedRating != -1) {

            when (selectedRating) {
                1 -> {
                    selected?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.recyclerViewBackground)
                    feedback1star?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.white)
                }

                2 -> {
                    selected?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.recyclerViewBackground)
                    feedback2star?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.white)
                }

                3 -> {
                    selected?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.recyclerViewBackground)
                    feedback3star?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.white)
                }

                4 -> {
                    selected?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.recyclerViewBackground)
                    feedback4star?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.white)
                }

                5 -> {
                    selected?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.recyclerViewBackground)
                    feedback5star?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.white)
                }
            }

        } else {
            selected?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.recyclerViewBackground)
        }
        selectedRating = currentViewID

    }

    private fun checkInputs(dialog: Dialog, toReport: Boolean) {
        val email: EditText
        val bodyText: EditText

        if (toReport) {
            email = dialog.findViewById(R.id.report_bug_user_email)
            bodyText = dialog.findViewById(R.id.report_bug_text)
        } else {
            email = dialog.findViewById(R.id.user_email_feedback)
            bodyText = dialog.findViewById(R.id.send_feedback_text)
        }

        if (!TextUtils.isEmpty(email.text.toString().trim())) {
            if (!TextUtils.isEmpty(bodyText.text.toString().trim())) {

                if (toReport) {
                    sendEmail(dialog, bodyText, email.text.toString(), true)

                } else {
                    if (selectedRating != -1) {
                        sendEmail(dialog, bodyText, email.text.toString(), false)
                    } else {
                        Toast.makeText(context, getString(R.string.rating_empty), Toast.LENGTH_SHORT).show()
                    }
                }

            } else {
                bodyText.requestFocus()
                Toast.makeText(context, getString(R.string.body_text_empty), Toast.LENGTH_SHORT).show()
            }
        } else {
            email.requestFocus()
            Toast.makeText(context, getString(R.string.email_empty), Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEmail(dialog: Dialog, bodyText: EditText, email: String, toReport: Boolean) {
        val body: String = bodyText.text.toString()
        val appName = resources.getString(R.string.app_name)

        val emailSubject: String
        val emailBody: String

        if (toReport) {
            emailSubject = "$appName - Report Bug Email From $email"
            emailBody = "User Email: $email\nBug Report:\n$body"
        } else {
            emailSubject = "$appName - Feedback Email From $email"
            emailBody = "User Email: $email\nUser Rating: $selectedRating Stars\n\nUser Suggestion:\n$body"
        }

        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(developerEmail))
        intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject)
        intent.putExtra(Intent.EXTRA_TEXT, emailBody)

        if (intent.resolveActivity(requireContext().packageManager) != null) {
            try {
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        bodyText.text = null

        if (!toReport) {
            setBackgroundColorOfTV(null, -1)
        }

        dialog.dismiss()
        Toast.makeText(context, getString(R.string.redirecting_to_email), Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestUpdateCode && resultCode != Activity.RESULT_OK) {
            Log.d("UPDATE", "Update flow failed! Result code: $resultCode")
        }
    }

}