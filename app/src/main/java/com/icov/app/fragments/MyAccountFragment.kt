package com.kabbodev.mongodb.fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.kabbodev.mongodb.R
import com.kabbodev.mongodb.activities.RegisterActivity
import com.kabbodev.mongodb.activities.UpdateUserInfoActivity
import com.kabbodev.mongodb.config.AppConfig
import com.kabbodev.mongodb.database.UserMongoDb
import com.kabbodev.mongodb.utils.Functions
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration

class MyAccountFragment : Fragment() {
    private val TAG = "My_ACCOUNT"

    private lateinit var app: App
    private lateinit var fullNameText: TextView
    private lateinit var emailIDText: TextView
    private lateinit var editAccountInfo: LinearLayout
    private lateinit var logOutBtn: Button

    private lateinit var loadingDialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_my_account, container, false)
        initializeVariables(view)
        setupClickListeners()
        return view
    }

    override fun onStart() {
        super.onStart()
        setupTheme()
    }

    private fun initializeVariables(view: View) {
        app = App(AppConfiguration.Builder(AppConfig.REALM_APP_ID).build())
        fullNameText = view.findViewById(R.id.username)
        emailIDText = view.findViewById(R.id.user_email)
        editAccountInfo = view.findViewById(R.id.edit_profile_info)
        logOutBtn = view.findViewById(R.id.log_out_btn)
        loadingDialog = Functions.createDialog(requireContext(), R.layout.loading_progress_dialog, false)
    }

    private fun setupTheme() {
        fullNameText.text = UserMongoDb.fullName
        emailIDText.text = UserMongoDb.email
    }

    private fun setupClickListeners() {
        editAccountInfo.setOnClickListener {
            Functions.startIntent(requireActivity(), UpdateUserInfoActivity::class.java, false)
        }
        logOutBtn.setOnClickListener {
            logOut()
        }
    }

    private fun logOut() {
        loadingDialog.show()
        app.currentUser()?.logOutAsync() { result ->
            if (result.isSuccess) {
                loadingDialog.dismiss()
                Functions.startIntent(requireActivity(), RegisterActivity::class.java, true)
                Log.d(TAG, "Successfully logged out.")
            } else {
                loadingDialog.dismiss()
                Toast.makeText(
                    requireContext(),
                    "Failed to log out, error: ${result.error}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d(TAG, "Failed to log out, error: ${result.error}")
            }
        }
    }

}