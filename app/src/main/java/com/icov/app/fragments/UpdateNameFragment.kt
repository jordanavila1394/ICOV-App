package com.kabbodev.mongodb.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.kabbodev.mongodb.R
import com.kabbodev.mongodb.config.AppConfig
import com.kabbodev.mongodb.database.UserMongoDb
import com.kabbodev.mongodb.utils.Functions
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.mongo.options.UpdateOptions
import org.bson.Document

class UpdateNameFragment : Fragment() {

    private val TAG = "UPDATE_NAME"
    private lateinit var app: App

    private lateinit var firstName: TextInputLayout
    private lateinit var surname: TextInputLayout
    private lateinit var updateBtn: Button

    private lateinit var loadingDialog: Dialog
    private lateinit var passwordConfirmationDialog: Dialog
    private lateinit var passwordText: TextInputLayout
    private lateinit var doneBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_update_name, container, false)
        initializeVariables(view)
        setupTheme()
        setupClickListeners()
        return view
    }

    private fun initializeVariables(view: View) {
        app = App(AppConfiguration.Builder(AppConfig.REALM_APP_ID).build())
        firstName = view.findViewById(R.id.first_name)
        surname = view.findViewById(R.id.surname)
        updateBtn = view.findViewById(R.id.update)
        loadingDialog =
            Functions.createDialog(requireContext(), R.layout.loading_progress_dialog, false)
        passwordConfirmationDialog =
            Functions.createDialog(requireContext(), R.layout.password_confirmation_dialog, true)
        passwordText = passwordConfirmationDialog.findViewById(R.id.password)
        doneBtn = passwordConfirmationDialog.findViewById(R.id.done_btn)
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
        firstName.editText?.addTextChangedListener(textWatcher)
        surname.editText?.addTextChangedListener(textWatcher)

        firstName.editText?.setText(UserMongoDb.firstName)
        surname.editText?.setText(UserMongoDb.surname)
    }

    private fun setupClickListeners() {
        updateBtn.setOnClickListener {
            checkFullName()
        }
    }

    private fun checkInputs() {
        if (!TextUtils.isEmpty(firstName.editText?.text)) {
            if (!TextUtils.isEmpty(surname.editText?.text)) {
                updateBtn.isEnabled = true
                updateBtn.setTextColor(resources.getColor(R.color.white))
            } else {
                updateBtn.isEnabled = false
                updateBtn.setTextColor(Color.argb(50, 255, 255, 255))
            }
        } else {
            updateBtn.isEnabled = false
            updateBtn.setTextColor(Color.argb(50, 255, 255, 255))
        }
    }

    private fun checkFullName() {
        val customErrorIcon = ContextCompat.getDrawable(requireContext(), R.drawable.error_icon)
        customErrorIcon!!.setBounds(
            -16,
            0,
            customErrorIcon.intrinsicWidth - 16,
            customErrorIcon.intrinsicHeight
        )

        if (firstName.editText?.text.toString() != UserMongoDb.firstName) {
            firstName.isErrorEnabled = false
            surname.isErrorEnabled = false

            if (surname.editText?.text.toString() != UserMongoDb.surname) {
                surname.isErrorEnabled = false
                checkPassword(updateFirstName = true, updateSurname = true, customErrorIcon)
            } else {
                checkPassword(updateFirstName = true, updateSurname = false, customErrorIcon)
            }

        } else {
            if (surname.editText?.text.toString() != UserMongoDb.surname) {
                firstName.isErrorEnabled = false
                surname.isErrorEnabled = false
                checkPassword(updateFirstName = false, updateSurname = true, customErrorIcon)

            } else {
                firstName.isErrorEnabled = true
                firstName.error = getString(R.string.name_same)
                firstName.errorIconDrawable = customErrorIcon

                surname.isErrorEnabled = true
                surname.error = getString(R.string.surname_same)
                surname.errorIconDrawable = customErrorIcon
            }
        }

    }

    private fun checkPassword(
        updateFirstName: Boolean,
        updateSurname: Boolean,
        customErrorIcon: Drawable
    ) {
        passwordConfirmationDialog.show()

        doneBtn.setOnClickListener {
            if (!TextUtils.isEmpty(passwordText.editText?.text)) {
                passwordText.isErrorEnabled = false

                if (passwordText.editText?.length()!! >= 8) {
                    passwordText.isErrorEnabled = false

                    if (passwordText.editText?.text.toString() == UserMongoDb.password) {
                        passwordText.isErrorEnabled = false

                        passwordConfirmationDialog.dismiss()
                        loadingDialog.show()

                        updateDatabase(updateFirstName, updateSurname)

                    } else {
                        passwordText.isErrorEnabled = true
                        passwordText.error = getString(R.string.incorrect_pass)
                        passwordText.errorIconDrawable = customErrorIcon
                    }
                } else {
                    passwordText.isErrorEnabled = true
                    passwordText.error = getString(R.string.pass_text_8_chars)
                    passwordText.errorIconDrawable = customErrorIcon
                }
            } else {
                passwordText.isErrorEnabled = true
                passwordText.error = getString(R.string.pass_cant_empty)
                passwordText.errorIconDrawable = customErrorIcon
            }
        }
    }

    private fun updateDatabase(updateFirstName: Boolean, updateSurname: Boolean) {
        val firstNameValue = firstName.editText?.text.toString()
        val surnameValue = surname.editText?.text.toString()

        val user = app.currentUser()!!
        val mongoClient = user.getMongoClient("mongodb-atlas")
        val mongoCollection = mongoClient.getDatabase("icovDB").getCollection("users")
        val queryFilter = Document("user_id", user.id)

        val updateDocument = Document("user_id", user.id)
            .append("name", firstNameValue)
            .append("surname", surnameValue)
            .append("email", UserMongoDb.email)
            .append("password", UserMongoDb.password)

        mongoCollection.findOneAndUpdate(queryFilter, updateDocument).getAsync { result ->
            if (result.isSuccess) {
                    Log.d(TAG, "successfully updated a document.")

                    if (updateFirstName) {
                        if (updateSurname) {
                            Log.d(TAG, "successfully updated first name and surname.")
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.name_both_updated),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Log.d(TAG, "successfully updated first name.")
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.name_updated),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Log.d(TAG, "successfully updated surname.")
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.surname_updated),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    UserMongoDb.firstName = firstNameValue
                    UserMongoDb.surname = surnameValue
                    UserMongoDb.fullName = "$firstNameValue $surnameValue"

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