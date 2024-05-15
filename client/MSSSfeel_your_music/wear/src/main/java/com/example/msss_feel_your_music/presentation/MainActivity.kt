/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.example.msss_feel_your_music.presentation

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.msss_feel_your_music.R
import com.example.msss_feel_your_music.presentation.theme.MSSSfeel_your_musicTheme
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() , MessageClient.OnMessageReceivedListener{
    private lateinit var messageClient: MessageClient


    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContent {
            WearApp("Android")
        }
        messageClient = Wearable.getMessageClient(this)
        messageClient.addListener(this)
    }


    override fun onMessageReceived(p0: MessageEvent) {
        val data = String(p0.data)
        when (data) {
            "start" -> {
                Log.d(ContentValues.TAG, "Received start command from mobile device")
                requestBodySensorsPermissionAndStartService()
            }
            "stop" -> {
                Log.d(ContentValues.TAG, "Received stop command from mobile device")
                stopSensorDataService()
            }
        }
    }

    private fun requestBodySensorsPermissionAndStartService() {
        val permission = Manifest.permission.BODY_SENSORS
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission)
        } else {
            startSensorDataService()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startSensorDataService()
        } else {
            Log.e(ContentValues.TAG, "Permission not granted for BODY_SENSORS")
        }
    }
    private fun startSensorDataService() {
        val intent = Intent(this, SensorDataService::class.java)
        intent.action = SensorDataService.ACTION_START
        startService(intent)
    }
    private fun stopSensorDataService() {
        val intent = Intent(this, SensorDataService::class.java)
        intent.action = SensorDataService.ACTION_STOP
        startService(intent)
    }


    companion object {
        private const val TAG = "MainActivity"

    }

}

@Composable
fun WearApp(greetingName: String) {
    MSSSfeel_your_musicTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            Greeting(greetingName = greetingName)
        }
    }
}

@Composable
fun Greeting(greetingName: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = stringResource(R.string.hello_world, greetingName)
    )
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview Android")
}

