package com.icov.app.fragments

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.icov.app.R
import com.icov.app.config.AppConfig
import com.icov.app.database.UserMongoDb
import com.icov.app.utils.Functions
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import org.bson.Document

class UpdatePasswordFragment : Fragment() {

    private val TAG = "UPDATE_PASS"
    private lateinit var app: App

    private lateinit var oldPassword: TextInputLayout
    private lateinit var newPassword: TextInputLayout
    private lateinit var confirmNewPassword: TextInputLayout
    private lateinit var updatePasswordBtn: Button
    private lateinit var loadingDialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_update_password, container, false)
        initializeVariables(view)
        setupTheme()
        setupClickListeners()
        return view
    }

    private fun initializeVariables(view: View) {
        app = App(AppConfiguration.Builder(AppConfig.REALM_APP_ID).build())
        oldPassword = view.findViewById(R.id.old_password)
        newPassword = view.findViewById(R.id.new_password)
        confirmNewPassword = view.findViewById(R.id.confirm_new_password)
        updatePasswordBtn = view.findViewById(R.id.update_password_btn)
        loadingDialog =
            Functions.createDialog(requireContext(), R.layout.loading_progress_dialog, false)
    }

    private fun setupTheme() {
        val textWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkInputs()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
        oldPassword.editText?.addTextChangedListener(textWatcher)
        newPassword.editText?.addTextChangedListener(textWatcher)
        confirmNewPassword.editText?.addTextChangedListener(textWatcher)
    }

    private fun setupClickListeners() {
        updatePasswordBtn.setOnClickListener {
            checkEmailAndPassword()
        }
    }

    private fun checkInputs() {
        if (!TextUtils.isEmpty(oldPassword.editText?.text) && oldPassword.editText?.length()!! >= 8) {
            if (!TextUtils.isEmpty(newPassword.editText?.text) && newPassword.editText?.length()!! >= 8) {
                if (!TextUtils.isEmpty(confirmNewPassword.editText?.text) && confirmNewPassword.editText?.length()!! >= 8) {
                    updatePasswordBtn.isEnabled = true
                    updatePasswordBtn.setTextColor(resources.getColor(R.color.white))
                } else {
                    updatePasswordBtn.isEnabled = false
                    updatePasswordBtn.setTextColor(Color.argb(50, 255, 255, 255))
                }
            } else {
                updatePasswordBtn.isEnabled = false
                updatePasswordBtn.setTextColor(Color.argb(50, 255, 255, 255))
            }
        } else {
            updatePasswordBtn.isEnabled = false
            updatePasswordBtn.setTextColor(Color.argb(50, 255, 255, 255))
        }
    }

    private fun checkEmailAndPassword() {
        val customErrorIcon = ContextCompat.getDrawable(requireContext(), R.drawable.error_icon)
        customErrorIcon!!.setBounds(
            -16,
            0,
            customErrorIcon.intrinsicWidth - 16,
            customErrorIcon.intrinsicHeight
        )

        if (newPassword.editText?.text.toString() == confirmNewPassword.editText?.text.toString()) {
            confirmNewPassword.isErrorEnabled = false

            if (oldPassword.editText?.text.toString() != newPassword.editText?.text.toString()) {
                newPassword.isErrorEnabled = false

                if (oldPassword.editText?.text.toString() == UserMongoDb.password) {
                    oldPassword.isErrorEnabled = false
                    loadingDialog.show()

                    val args: List<String> =
                        listOf("oldPass", oldPassword.editText?.text.toString())
                    app.emailPassword.callResetPasswordFunctionAsync(
                        UserMongoDb.email, newPassword.editText?.text.toString(),
                        arrayOf(args)
                    ) { result ->
                        if (result.isSuccess) {
                            Log.d(TAG, "Successfully updated password for user.")
                            updateDatabase()

                        } else {
                            Log.d(TAG, "Failed to reset user's password.")
                            loadingDialog.dismiss()
                        }
                    }

                } else {
                    oldPassword.isErrorEnabled = true
                    oldPassword.error = getString(R.string.incorrect_pass)
                    oldPassword.errorIconDrawable = customErrorIcon
                }
            } else {
                newPassword.isErrorEnabled = true
                newPassword.error = getString(R.string.pass_same)
                newPassword.errorIconDrawable = customErrorIcon
            }
        } else {
            confirmNewPassword.isErrorEnabled = true
            confirmNewPassword.error = getString(R.string.password_doesnt_match)
            confirmNewPassword.errorIconDrawable = customErrorIcon
        }
    }

    private fun updateDatabase() {
        val passwordValue = newPassword.editText?.text.toString()

        val user = app.currentUser()!!
        val mongoClient = user.getMongoClient("mongodb-atlas")
        val mongoCollection = mongoClient.getDatabase("icovDB").getCollection("users")
        val queryFilter = Document("user_id", user.id)

        val updateDocument = Document("user_id", user.id)
            .append("name", UserMongoDb.firstName)
            .append("surname", UserMongoDb.surname)
            .append("email", UserMongoDb.email)
            .append("password", passwordValue)

        mongoCollection.findOneAndUpdate(queryFilter, updateDocument).getAsync { result ->
            if (result.isSuccess) {
                Log.d(TAG, "successfully updated a document.")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.pass_updated),
                    Toast.LENGTH_SHORT
                ).show()

                UserMongoDb.password = passwordValue

                loadingDialog.dismiss()
                requireActivity().finish()
            } else {
                Log.d(TAG, "did not update a document.")
                Toast.makeText(
                    requireContext(),
                    "Failed to update ${result.error}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

}