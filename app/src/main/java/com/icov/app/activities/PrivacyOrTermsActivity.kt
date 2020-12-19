package com.icov.app.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.icov.app.R

class PrivacyOrTermsActivity : AppCompatActivity() {

    private val assertLoc = "file:///android_asset/"
    private val privacyPolicyFileName = "PrivacyPolicy.html"
    private val termsAndConditionsFileName = "TermsAndConditions.html"

    private lateinit var privacyImg: ImageView
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val pref = PreferenceManager.getDefaultSharedPreferences(this)
//        Functions.setLocale(this, pref.getString("language", "en"))
        setContentView(R.layout.activity_privacy_or_terms)

        initializeVariables()
        setupTheme()
    }

    private fun initializeVariables() {
        privacyImg = findViewById(R.id.privacy_image)
        webView = findViewById(R.id.web_view)
    }

    private fun setupTheme() {
        val toolbar: Toolbar = findViewById(R.id.custom_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        webView.settings.javaScriptEnabled = true

        if (intent.getStringExtra("type") == "Privacy") {
            supportActionBar?.title = getString(R.string.privacy_policy_title)
            privacyImg.visibility = View.VISIBLE
            webView.loadUrl(assertLoc + privacyPolicyFileName)

        } else {
            supportActionBar?.title = getString(R.string.terms_and_conditions_title)
            privacyImg.visibility = View.GONE
            webView.loadUrl(assertLoc + termsAndConditionsFileName)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}