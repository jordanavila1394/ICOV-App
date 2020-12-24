package com.icov.app.activities

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.TransitionManager
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.icov.app.R
import com.icov.app.config.AppConfig
import com.icov.app.databinding.ActivityResetPasswordBinding
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var bind: ActivityResetPasswordBinding
    private lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(bind.root)
        setupTheme()
        setupClickListeners()
    }

    private fun setupTheme() {
        app = App(AppConfiguration.Builder(AppConfig.REALM_APP_ID).build())
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkInputs()
            }

            override fun afterTextChanged(s: Editable?) {}
        }
        bind.forgotPassEmail.addTextChangedListener(textWatcher)
    }

    private fun setupClickListeners() {
        bind.resetPassBtn.setOnClickListener {
            sendResetPassEmail()
        }
        bind.tvGoBack.setOnClickListener {
            finish()
        }
    }

    private fun sendResetPassEmail() {
        TransitionManager.beginDelayedTransition(bind.emailIconContainer)
        bind.emailIconText.visibility = View.GONE

        TransitionManager.beginDelayedTransition(bind.emailIconContainer)
        bind.emailIcon.visibility = View.VISIBLE
        bind.progressBar.visibility = View.VISIBLE

        bind.resetPassBtn.isEnabled = false
        bind.resetPassBtn.setTextColor(Color.argb(50, 255, 255, 255))

        app
            .emailPassword
            .sendResetPasswordEmailAsync(bind.forgotPassEmail.text.toString()) { result ->
                if (result.isSuccess) {
                    val scaleAnimation = ScaleAnimation(
                        1F,
                        0F,
                        1F,
                        0F,
                        (bind.emailIcon.width / 2).toFloat(),
                        (bind.emailIcon.height / 2).toFloat()
                    )
                    scaleAnimation.duration = 100
                    scaleAnimation.interpolator = AccelerateInterpolator()
                    scaleAnimation.repeatMode = Animation.REVERSE
                    scaleAnimation.repeatCount = 1

                    val animationListener = object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}

                        override fun onAnimationEnd(animation: Animation?) {
                            bind.emailIconText.text =
                                getString(R.string.recovery_email_sent)
                            bind.emailIconText.setTextColor(
                                ContextCompat.getColor(
                                    this@ResetPasswordActivity,
                                    R.color.green
                                )
                            )
                            TransitionManager.beginDelayedTransition(bind.emailIconContainer)
                            bind.emailIconText.visibility = View.VISIBLE
                        }

                        override fun onAnimationRepeat(animation: Animation?) {
                            bind.emailIcon.setImageResource(R.drawable.green_email)
                        }
                    }

                    scaleAnimation.setAnimationListener(animationListener)

                    bind.emailIcon.startAnimation(scaleAnimation)
                    bind.progressBar.visibility = View.GONE

                } else {
                    bind.emailIcon.setImageResource(R.drawable.red_email)
                    bind.resetPassBtn.isEnabled = true
                    bind.resetPassBtn.setTextColor(ContextCompat.getColor(this, R.color.white))
                    bind.emailIconText.text = result.error.message
                    bind.emailIconText.setTextColor(
                        ContextCompat.getColor(
                            this@ResetPasswordActivity,
                            R.color.red
                        )
                    )
                    TransitionManager.beginDelayedTransition(bind.emailIconContainer)
                    bind.emailIconText.visibility = View.VISIBLE
                    bind.progressBar.visibility = View.GONE
                }
            }
    }

    private fun checkInputs() {
        if (bind.forgotPassEmail.text.isNotEmpty()) {
            bind.resetPassBtn.isEnabled = true
            bind.resetPassBtn.setTextColor(ContextCompat.getColor(this, R.color.white))
            return
        }
        bind.resetPassBtn.isEnabled = false
        bind.resetPassBtn.setTextColor(Color.argb(50, 255, 255, 255))
    }

}