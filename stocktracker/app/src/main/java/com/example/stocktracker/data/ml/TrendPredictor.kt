package com.example.stocktracker.data.ml

import android.content.Context
import android.util.Log
import com.example.stocktracker.domain.model.TrendDirection
import com.example.stocktracker.domain.model.TrendPrediction
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrendPredictor @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "TrendPredictor"
        private const val MODEL_FILE = "trend_model.tflite"
        private const val INPUT_SIZE = 10
        private const val OUTPUT_CLASSES = 3
    }

    private var interpreter: Interpreter? = null
    private val modelLock = Object()

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            val options = Interpreter.Options().apply {
                numThreads = 4
                useNNAPI = true
                // Enable GPU delegate if available
                // addDelegate(GpuDelegate())
            }
            interpreter = Interpreter(modelBuffer, options)
            Log.d(TAG, "TFLite model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: ${e.message}")
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(features: FloatArray): TrendPrediction? {
        synchronized(modelLock) {
            val interp = interpreter ?: return null

            return try {
                val input = Array(1) { features }
                val output = Array(1) { FloatArray(OUTPUT_CLASSES) }

                interp.run(input, output)

                val probabilities = output[0]
                val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 2

                val direction = when (maxIndex) {
                    0 -> TrendDirection.UP
                    1 -> TrendDirection.DOWN
                    else -> TrendDirection.NEUTRAL
                }

                TrendPrediction(
                    direction = direction,
                    confidence = probabilities[maxIndex],
                    indicators = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Prediction failed: ${e.message}")
                null
            }
        }
    }

    fun isModelLoaded(): Boolean = interpreter != null

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}