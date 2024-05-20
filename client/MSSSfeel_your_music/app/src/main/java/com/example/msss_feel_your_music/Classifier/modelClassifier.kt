package com.example.msss_feel_your_music.Classifier

import android.content.ContentValues.TAG
import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class ModelClassifier(applicationContext: Context) {

    private var interpreter: Interpreter? = null

    init {
        try {
            val assetManager = applicationContext.assets
            val model = loadModelFile(assetManager, "model.tflite")
            interpreter = Interpreter(model)
            if (interpreter != null) {
                Log.d(TAG, "Model loaded correctly")
            } else {
                Log.e(TAG, "Error during classifier loading")
            }
            Log.d(TAG, "tensor Input : ${interpreter?.inputTensorCount}")
            Log.d(TAG, "tensor Output:  ${interpreter?.outputTensorCount}")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(context: AssetManager, modelPath: String): ByteBuffer {
        val fileDescriptor = context.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            .order(ByteOrder.nativeOrder())
    }

    fun classify(features: List<Float>): Int {
        Log.d(TAG,"features size ${features.size}")
        val inputBuffer = ByteBuffer.allocateDirect(features.size * Float.SIZE_BYTES)
        inputBuffer.order(ByteOrder.nativeOrder())

        for (feature in features) {
            inputBuffer.putFloat(feature)
        }
        Log.d(TAG,"InputBuffer  $inputBuffer")


        val output = Array(1) { FloatArray(OUTPUT_CLASSES_COUNT) }

        interpreter?.run(inputBuffer, output)
        for (i in output.indices) {
            for (j in output[i].indices) {
                println("Output prob[$i][$j]: ${output[i][j]}")
            }
        }
        val maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1
        return maxIndex
    }

    companion object {
        private const val OUTPUT_CLASSES_COUNT = 5
    }


}