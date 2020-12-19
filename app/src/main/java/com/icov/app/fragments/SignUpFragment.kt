package com.icov.app.fragments

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.icov.app.R
import com.icov.app.activities.MainActivity
import com.icov.app.config.AppConfig
import com.icov.app.utils.Functions
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.Credentials
import org.bson.Document

class SignUpFragment : Fragment() {
    private val TAG = "AUTH"

    private lateinit var app: App

    private lateinit var firstName: TextInputLayout
    private lateinit var surname: TextInputLayout
    private lateinit var emailID: TextInputLayout
    private lateinit var password: TextInputLayout
    private lateinit var confirmPassword: TextInputLayout

    private lateinit var signUpProgressBar: ProgressBar
    private lateinit var signUpProgressText: TextView
    private lateinit var signUpBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_sign_up, container, false)
        initializeVariables(view)
        setupClickListeners()
        return view
    }

    private fun initializeVariables(view: View) {
        app = App(AppConfiguration.Builder(AppConfig.REALM_APP_ID).build())
        firstName = view.findViewById(R.id.sign_up_first_name)
        surname = view.findViewById(R.id.sign_up_surname)
        emailID = view.findViewById(R.id.sign_up_email)
        password = view.findViewById(R.id.sign_up_password)
        confirmPassword = view.findViewById(R.id.sign_up_confirm_password)
        signUpProgressBar = view.findViewById(R.id.sign_up_progress_bar)
        signUpProgressText = view.findViewById(R.id.sign_up_progress_text)
        signUpBtn = view.findViewById(R.id.sign_up_btn)
    }

    private fun setupClickListeners() {
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
        emailID.editText?.addTextChangedListener(textWatcher)
        password.editText?.addTextChangedListener(textWatcher)
        confirmPassword.editText?.addTextChangedListener(textWatcher)

        signUpBtn.setOnClickListener {
            checkEmailAndPassword()
        }
    }

    private fun checkInputs() {
        if (!TextUtils.isEmpty(firstName.editText?.text)) {
            if (!TextUtils.isEmpty(surname.editText?.text)) {
                if (!TextUtils.isEmpty(emailID.editText?.text)) {
                    if (!TextUtils.isEmpty(password.editText?.text) && password.editText?.length()!! >= 8) {
                        if (!TextUtils.isEmpty(confirmPassword.editText?.text)) {
                            signUpBtn.isEnabled = true
                            signUpBtn.setTextColor(resources.getColor(R.color.white))
                        } else {
                            signUpBtn.isEnabled = false
                            signUpBtn.setTextColor(Color.argb(50, 255, 255, 255))
                        }
                    } else {
                        signUpBtn.isEnabled = false
                        signUpBtn.setTextColor(Color.argb(50, 255, 255, 255))
                    }
                } else {
                    signUpBtn.isEnabled = false
                    signUpBtn.setTextColor(Color.argb(50, 255, 255, 255))
                }
            } else {
                signUpBtn.isEnabled = false
                signUpBtn.setTextColor(Color.argb(50, 255, 255, 255))
            }
        } else {
            signUpBtn.isEnabled = false
            signUpBtn.setTextColor(Color.argb(50, 255, 255, 255))
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

        if (Patterns.EMAIL_ADDRESS.matcher(emailID.editText?.text.toString()).matches()) {
            emailID.isErrorEnabled = false

            if (password.editText?.text.toString() == confirmPassword.editText?.text.toString()) {
                confirmPassword.isErrorEnabled = false
                signUpProgressText.visibility = View.VISIBLE
                signUpProgressText.text = getString(R.string.registering_text)
                signUpProgressBar.visibility = View.VISIBLE
                signUpBtn.isEnabled = false
                signUpBtn.setTextColor(Color.argb(50, 255, 255, 255))

                signUp()

            } else {
                confirmPassword.isErrorEnabled = true
                confirmPassword.error = getString(R.string.password_doesnt_match)
                confirmPassword.errorIconDrawable = customErrorIcon
            }
        } else {
            emailID.isErrorEnabled = true
            emailID.error = getString(R.string.invalid_email_address)
            emailID.errorIconDrawable = customErrorIcon
        }
    }

    private fun signUp() {
        val emailValue = emailID.editText?.text.toString()
        val passwordValue = password.editText?.text.toString()

        app.emailPassword
            .registerUserAsync(emailValue, passwordValue) { result ->
                if (result.isSuccess) {
                    signUpProgressText.text =
                        getString(R.string.successfully_registered_text)
                    Log.d(TAG, "Successfully registered user.")
                    val emailPasswordCredentials: Credentials =
                        Credentials.emailPassword(emailValue, passwordValue)

                    app
                        .loginAsync(emailPasswordCredentials) { loginResult ->
                            if (loginResult.isSuccess) {
                                signUpProgressText.text =
                                    getString(R.string.successfully_logged_in_text)
                                setDataOnDB(emailValue, passwordValue)

                            } else {
                                Log.d(TAG, "Failed to login user: ${result.error}")
                                signUpProgressText.visibility = View.INVISIBLE
                                signUpProgressText.text = getString(R.string.registering_text)
                                signUpProgressBar.visibility = View.INVISIBLE
                                signUpBtn.isEnabled = true
                                signUpBtn.setTextColor(resources.getColor(R.color.white))
                                Toast.makeText(
                                    requireContext(),
                                    result.error.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                } else {
                    Log.d(TAG, "Failed to register user: ${result.error}")
                    signUpProgressText.visibility = View.INVISIBLE
                    signUpProgressText.text = getString(R.string.registering_text)
                    signUpProgressBar.visibility = View.INVISIBLE
                    signUpBtn.isEnabled = true
                    signUpBtn.setTextColor(resources.getColor(R.color.white))
                    Toast.makeText(
                        requireContext(),
                        result.error.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun setDataOnDB(emailValue: String, passwordValue: String) {
        val user = app.currentUser()!!
        val mongoClient = user.getMongoClient("mongodb-atlas")
        val mongoCollection = mongoClient.getDatabase("icovDB").getCollection("users")

        val firstNameValue = firstName.editText?.text.toString()
        val surnameValue = surname.editText?.text.toString()

        val document = Document("user_id", user.id)
            .append("name", firstNameValue)
            .append("surname", surnameValue)
            .append("email", emailValue)
            .append("password", passwordValue)

        mongoCollection
            .insertOne(document)
            .getAsync { result ->
                if (result.isSuccess) {
                    Log.d(TAG, "Successfully created the document!")
                    Functions.startIntent(
                        requireActivity(),
                        MainActivity::class.java,
                        true
                    )
                } else {
                    Log.d(TAG, "Failed to create document: ${result.error}")
                    signUpProgressText.visibility = View.INVISIBLE
                    signUpProgressText.text = getString(R.string.registering_text)
                    signUpProgressBar.visibility = View.INVISIBLE
                    signUpBtn.isEnabled = true
                    signUpBtn.setTextColor(resources.getColor(R.color.white))
                    Toast.makeText(
                        requireContext(),
                        result.error.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

}