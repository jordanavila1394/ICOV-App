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
import com.icov.app.activities.ResetPasswordActivity
import com.icov.app.config.AppConfig
import com.icov.app.utils.Functions
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.Credentials

class SignInFragment : Fragment() {
    private val TAG = "AUTH"

    private lateinit var app: App

    private lateinit var emailID: TextInputLayout
    private lateinit var password: TextInputLayout
    private lateinit var forgotPassword: TextView

    private lateinit var progressBar: ProgressBar
    private lateinit var signInBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_sign_in, container, false)

        initializeVariables(view)
        setupTheme()
        setupClickListeners()

        return view
    }

    private fun initializeVariables(view: View) {
        app = App(AppConfiguration.Builder(AppConfig.REALM_APP_ID).build())
        emailID = view.findViewById(R.id.sign_in_email)
        password = view.findViewById(R.id.sign_in_password)
        forgotPassword = view.findViewById(R.id.forgot_password)
        progressBar = view.findViewById(R.id.sign_in_progress_bar)
        signInBtn = view.findViewById(R.id.sign_in_btn)
    }

    private fun setupTheme() {
        emailID.translationX = 800f
        password.translationX = 800f
        forgotPassword.translationX = 800f
        signInBtn.translationX = 800f

        emailID.alpha = 0f
        password.alpha = 0f
        forgotPassword.alpha = 0f
        signInBtn.alpha = 0f

        emailID.animate().translationX(0f).alpha(1f).setDuration(800).setStartDelay(300).start()
        password.animate().translationX(0f).alpha(1f).setDuration(800).setStartDelay(500).start()
        forgotPassword.animate().translationX(0f).alpha(1f).setDuration(800).setStartDelay(500)
            .start()
        signInBtn.animate().translationX(0f).alpha(1f).setDuration(800).setStartDelay(700).start()
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

        emailID.editText?.addTextChangedListener(textWatcher)
        password.editText?.addTextChangedListener(textWatcher)

        forgotPassword.setOnClickListener {
            Functions.startIntent(requireActivity(), ResetPasswordActivity::class.java, false)
        }
        signInBtn.setOnClickListener {
            checkEmailAndPassword()
        }
    }

    private fun checkInputs() {
        if (!TextUtils.isEmpty(emailID.editText?.text)) {
            if (!TextUtils.isEmpty(password.editText?.text)) {
                signInBtn.isEnabled = true
                signInBtn.setTextColor(resources.getColor(R.color.white))
            } else {
                signInBtn.isEnabled = false
                signInBtn.setTextColor(Color.argb(50, 255, 255, 255))
            }
        } else {
            signInBtn.isEnabled = false
            signInBtn.setTextColor(Color.argb(50, 255, 255, 255))
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

            if (password.editText?.length()!! >= 8) {
                password.isErrorEnabled = false

                progressBar.visibility = View.VISIBLE
                signInBtn.isEnabled = false
                signInBtn.setTextColor(Color.argb(50, 255, 255, 255))

                val emailPasswordCredentials: Credentials = Credentials.emailPassword(
                    emailID.editText?.text.toString(),
                    password.editText?.text.toString()
                )

                app.loginAsync(emailPasswordCredentials) {result ->
                    if (result.isSuccess) {
                        Log.d(TAG, "Successfully authenticated using an email and password.")
                        Functions.startIntent(requireActivity(), MainActivity::class.java, true)
                    } else {
                        Log.d(TAG, result.error.toString())
                        progressBar.visibility = View.INVISIBLE
                        signInBtn.isEnabled = true
                        signInBtn.setTextColor(resources.getColor(R.color.white))
                        Toast.makeText(requireContext(), result.error.toString(), Toast.LENGTH_SHORT).show()
                    }
                }

            } else {
                password.isErrorEnabled = true
                password.error = getString(R.string.incorrect_pass)
                password.errorIconDrawable = customErrorIcon
            }

        } else {
            emailID.isErrorEnabled = true
            emailID.error = getString(R.string.invalid_email_address)
            emailID.errorIconDrawable = customErrorIcon
        }
    }

}