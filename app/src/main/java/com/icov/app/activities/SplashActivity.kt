package com.icov.app.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.icov.app.R
import com.icov.app.config.AppConfig
import io.realm.Realm
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.User
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Realm.init(this)

        val app = App(AppConfiguration.Builder(AppConfig.REALM_APP_ID).build())
        val currentUser: User? = app.currentUser()

        GlobalScope.launch(Dispatchers.IO) {
            delay(1500)
            withContext(Dispatchers.Main) {
                val intent =
                    if (currentUser == null) {
                        Intent(this@SplashActivity, RegisterActivity::class.java)
                    } else {
                        Intent(this@SplashActivity, MainActivity::class.java)
                    }
                startActivity(intent)
                finish()
            }
        }
    }

}