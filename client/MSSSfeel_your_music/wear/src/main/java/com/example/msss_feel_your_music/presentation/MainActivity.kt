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
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.msss_feel_your_music.R
import com.example.msss_feel_your_music.presentation.theme.MSSSfeel_your_musicTheme
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() , MessageClient.OnMessageReceivedListener {

    private lateinit var messageClient: MessageClient
    private var isStartButtonVisible by mutableStateOf(false)
    private var isCaptureActive by mutableStateOf(false)



    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContent {
            WearApp("Android", isStartButtonVisible,isCaptureActive) {
                if(isStartButtonVisible) {
                    startSensorDataService()
                }

            }
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

//            "stop" -> {
//                Log.d(ContentValues.TAG, "Received stop command from mobile device")
//                stopSensorDataService()
//            }
        }
    }

    private fun requestBodySensorsPermissionAndStartService() {
        val permission = Manifest.permission.BODY_SENSORS
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(permission)
        } else {
            isStartButtonVisible = true
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                isStartButtonVisible = true
                isCaptureActive=false
            } else {
                Log.e(ContentValues.TAG, "Permission not granted for BODY_SENSORS")
            }
        }

    private fun startSensorDataService() {
        val intent = Intent(this, SensorDataService::class.java)
//        intent.action = SensorDataService.ACTION_START
        startService(intent)
        isCaptureActive= true
        isStartButtonVisible=false
    }

    private fun stopSensorDataService() {
        val intent = Intent(this, SensorDataService::class.java)
        stopService(intent)
        isCaptureActive=false
        isStartButtonVisible=false
    }


    companion object {
        private const val TAG = "MainActivity"

    }

    @Composable
    fun WearApp(greetingName: String, isStartButtonVisible: Boolean,isCaptureActive: Boolean, onStartClicked: () -> Unit) {
        MSSSfeel_your_musicTheme{
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(id = R.color.background_blue)),
                contentAlignment = Alignment.Center
            ) {
                TimeText()
                Greeting(greetingName = greetingName)
                if (isStartButtonVisible) {
                    Button(
                        onClick = onStartClicked,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Black,
                            contentColor = White
                        )
                    ) {
                        Text(text = "Start")
                    }
                }
                if (isCaptureActive) {
                    Button(
                        onClick = { stopSensorDataService() },
                        modifier = Modifier.align(Alignment.BottomCenter),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Black,
                            contentColor = White
                        )

                    ) {
                        Text(text = "Stop")
                    }
                }
            }

        }
    }

    @Composable
    fun Greeting(greetingName: String) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = colorResource(id = R.color.text_black),
            text = stringResource(R.string.hello_world, greetingName),
            fontWeight = FontWeight.Bold

        )
    }

    @Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
    @Composable
    fun DefaultPreview() {
        WearApp("Preview Android",true,true) {}
    }
}