package com.kabbodev.mongodb.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.kabbodev.mongodb.R
import com.kabbodev.mongodb.config.AppConfig
import io.realm.Realm
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.User

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Realm.init(this)

        val app = App(AppConfiguration.Builder(AppConfig.REALM_APP_ID).build())
        val currentUser: User? = app.currentUser()

        val handler = Handler()
        handler.postDelayed({
            val intent =
                if (currentUser == null) {
                    Intent(this@SplashActivity, RegisterActivity::class.java)
                } else {
                    Intent(this@SplashActivity, MainActivity::class.java)
                }
            startActivity(intent)
            finish()
        }, 1500)
    }


}