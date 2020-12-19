package com.icov.app.activities

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.*
import com.icov.app.R
import com.icov.app.config.AppConfig
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var app: App

    private lateinit var forgotPassRegisteredEmail: EditText
    private lateinit var resetPassBtn: Button
    private lateinit var forgotPassGoBack: TextView

    private lateinit var emailIconContainer: ViewGroup
    private lateinit var emailIcon: ImageView
    private lateinit var emailIconText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)
        initializeVariables()
        setupTheme()
        setupClickListeners()
    }

    private fun initializeVariables() {
        app = App(AppConfiguration.Builder(AppConfig.REALM_APP_ID).build())
        forgotPassRegisteredEmail = findViewById(R.id.forgot_pass_email)
        resetPassBtn = findViewById(R.id.reset_password_btn)
        forgotPassGoBack = findViewById(R.id.tv_forgot_password_go_back)
        emailIconContainer = findViewById(R.id.forgot_password_email_icon_container)
        emailIconText = findViewById(R.id.forgot_password_email_icon_text)
        emailIcon = findViewById(R.id.forgot_password_email_icon)
        progressBar = findViewById(R.id.forgot_password_progress_bar)
    }

    private fun setupTheme() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkInputs()
            }

            override fun afterTextChanged(s: Editable?) {}
        }
        forgotPassRegisteredEmail.addTextChangedListener(textWatcher)
    }

    private fun setupClickListeners() {
        resetPassBtn.setOnClickListener {
            TransitionManager.beginDelayedTransition(emailIconContainer)
            emailIconText.visibility = View.GONE

            TransitionManager.beginDelayedTransition(emailIconContainer)
            emailIcon.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE

            resetPassBtn.isEnabled = false
            resetPassBtn.setTextColor(Color.argb(50, 255, 255, 255))

            app
                .emailPassword
                .sendResetPasswordEmailAsync(forgotPassRegisteredEmail.text.toString()) { result ->
                if (result.isSuccess) {
                    val scaleAnimation = ScaleAnimation(1F, 0F, 1F, 0F, (emailIcon.width / 2).toFloat(), (emailIcon.height / 2).toFloat())
                    scaleAnimation.duration = 100
                    scaleAnimation.interpolator = AccelerateInterpolator()
                    scaleAnimation.repeatMode = Animation.REVERSE
                    scaleAnimation.repeatCount = 1

                    val animationListener = object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}

                        override fun onAnimationEnd(animation: Animation?) {
                            emailIconText.text = getString(R.string.recovery_email_sent)
                            emailIconText.setTextColor(resources.getColor(R.color.green))
                            TransitionManager.beginDelayedTransition(emailIconContainer)
                            emailIconText.visibility = View.VISIBLE
                        }

                        override fun onAnimationRepeat(animation: Animation?) {
                            emailIcon.setImageResource(R.drawable.green_email)
                        }
                    }

                    scaleAnimation.setAnimationListener(animationListener)

                    emailIcon.startAnimation(scaleAnimation)
                    progressBar.visibility = View.GONE

                } else {
                    emailIcon.setImageResource(R.drawable.red_email)
                    resetPassBtn.isEnabled = true
                    resetPassBtn.setTextColor(Color.rgb(255, 255, 255))
                    emailIconText.text = result.error.message
                    emailIconText.setTextColor(resources.getColor(R.color.red))
                    TransitionManager.beginDelayedTransition(emailIconContainer)
                    emailIconText.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                }
            }
        }

        forgotPassGoBack.setOnClickListener {
            finish()
        }
    }

    private fun checkInputs() {
        if (!TextUtils.isEmpty(forgotPassRegisteredEmail.text)) {
            resetPassBtn.isEnabled = true
            resetPassBtn.setTextColor(Color.rgb(255, 255, 255))
        } else {
            resetPassBtn.isEnabled = false
            resetPassBtn.setTextColor(Color.argb(50, 255, 255, 255))
        }
    }

}