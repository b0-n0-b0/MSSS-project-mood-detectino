package com.example.msss_feel_your_music.presentation

import kotlin.random.Random
//class to simulate value to test without the wearable device
class SensorSimulator {
    private var heartRateListeners = mutableListOf<(Float) -> Unit>()
    private var edaListeners = mutableListOf<(Float) -> Unit>()

    private var isRunning = false

    fun startSimulation() {
        if (!isRunning) {
            isRunning = true
            simulateData()
        }
    }

    fun stopSimulation() {
        isRunning = false
    }

    fun addHeartRateListener(listener: (Float) -> Unit) {
        heartRateListeners.add(listener)
    }

    fun addEDaListener(listener: (Float) -> Unit) {
        edaListeners.add(listener)
    }

    private fun simulateData() {
        Thread {
            while (isRunning) {
                val heartRate = generateHeartRate()
                val eda = generateEDA()

                heartRateListeners.forEach { it(heartRate) }
                edaListeners.forEach { it(eda) }

                Thread.sleep(2000)
            }
        }.start()
    }

    private fun generateHeartRate(): Float {
        return Random.nextFloat() * (90 - 40) + 40
    }

    private fun generateEDA(): Float {
        return Random.nextFloat() * 2
    }
}