/**
 * # WEEK10: RECEIVERS
 * A static receiver on boot for reminders and
 * dynamically broadcasting steps to be received by another app
 *
 * 1. adding a static broadcast receiver in the manifest
 * 2. creating a OnBootReceiver to do a deferred task when system BOOT_COMPLETED
 * 3. create an intent to be broadcasted to the world (in your device)
 */

package com.boliao.eod

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import java.lang.ref.WeakReference
import kotlinx.coroutines.*

/**
 * This is the splash view that records who is playing.
 */
class Splash : AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var startAndroidLauncher: Intent

    fun launchGame() {
        startActivity(startAndroidLauncher)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // init launch game intent
        startAndroidLauncher = Intent(this@Splash, AndroidLauncher::class.java)

        // get refs to UI components
        val playBtn = findViewById<Button>(R.id.play_btn)
        val usernameEdtTxt = findViewById<EditText>(R.id.name_edtxt)
        val msgTxtView = findViewById<TextView>(R.id.msg_txtview)
        val weatherTxtView = findViewById<TextView>(R.id.weather_txtview)

        // show splash text
        msgTxtView.setText(R.string.welcome_note)

        // TODO THREADING 2: create a persistent weather widget
        // An MVVM Splash ViewModel is already set up.
        // Splash Activity View -> Splash ViewModel -> WeatherRepo Model
        // WeatherRepo currently has a mock stub to return static mock data, provided live by
        // weatherData in SplashViewModel.
        // - set up weatherTextView here to observe the weatherData
        // - goto WeatherRepo for THREADING 3
        // Q: Do I (Splash Activity) need to know about WeatherRepo?

        val splashViewModel = ViewModelProviders.of(this).get(SplashViewModel::class.java)
        splashViewModel.weatherData.observe(
                this,
                Observer {
                    weatherTxtView.text = it
                }
        )

        splashViewModel.loginStatus.observe(this, Observer {
            if (it) {
                msgTxtView.text = "LOGIN DONE. Starting..."
                launchGame()
            } else {
                msgTxtView.text = "Name OREDI exist liao..."
            }
        })

        // start game on click "PLAY"
        playBtn.setOnClickListener {
            msgTxtView.text = "Encrypting in coroutine heaven..."
            splashViewModel.login(usernameEdtTxt.text.toString())
        }
    }

    companion object {
        private const val TAG = "Splash"

        /**
         * [DEPRECATED] AsyncTask to "encrypt" username
         * - heavy lifting in the background to be posted back to UI
         * - static class so as to prevent leaks
         * - internal ctor to only allow enclosing class to construct
         * - need a ref to update UI thread, so use WeakReference (a.k.a. shared_ptr)
         * - onProgressUpdate(Integer... progress) left as an exercise
         * - note: publishProgress(Integer) is in built to pass progress to above from doInBackground
         */
        /*
        private class EncryptTask internal constructor(act: Splash) : AsyncTask<String?, Void?, Boolean>() {
            // hold the Activity to get all the UI elements
            // - use weak reference so that it does not leak mem when activity gets killed
            var wr_splash: WeakReference<Splash> = WeakReference(act)

            override fun onPreExecute() {
                super.onPreExecute()
                val splash = wr_splash.get()
                if (splash != null) {
                    (splash.findViewById<View>(R.id.msg_txtview) as TextView).text = "encrypting"
                }
            }

            override fun doInBackground(vararg str: String?): Boolean {
                try {
                    Thread.sleep(3000)
                    // do something to the str
                } catch (e: InterruptedException) {
                    return false
                }
                return true
            }

            override fun onPostExecute(b: Boolean) {
                super.onPostExecute(b)
                val splash = wr_splash.get()
                if (splash != null) {
                    (splash.findViewById<View>(R.id.msg_txtview) as TextView).text = "The encryption is:$b"
                    splash.launchGame()
                }
            }
        }
         */
    }
}