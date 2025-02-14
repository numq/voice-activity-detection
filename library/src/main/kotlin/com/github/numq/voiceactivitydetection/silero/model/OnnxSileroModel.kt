package com.github.numq.voiceactivitydetection.silero.model

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

internal class OnnxSileroModel(modelPath: String, targetSampleRate: Int) : SileroModel {
    private companion object {
        const val CONTEXT_SIZE = 64
        const val BATCH_SIZE = 512
    }

    private val env by lazy { OrtEnvironment.getEnvironment() }

    private val srTensor by lazy { OnnxTensor.createTensor(env, longArrayOf(targetSampleRate.toLong())) }

    private val session by lazy {
        env.createSession(modelPath, OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.NO_OPT)
        })
    }

    private var state = Array(2) { Array(1) { FloatArray(128) } }

    private var context = emptyArray<FloatArray>()

    private var lastBatchSize = 0

    override fun process(input: Array<FloatArray>) = runCatching {
        val batchSize = input.size

        if (lastBatchSize == 0 || lastBatchSize != batchSize) reset(batchSize).getOrThrow()

        if (context.size != batchSize) {
            context = Array(batchSize) { FloatArray(CONTEXT_SIZE) }
        }

        val combinedInput = Array(batchSize) { i ->
            val currentContext = context.getOrNull(i) ?: FloatArray(CONTEXT_SIZE)

            val currentInput = input.getOrNull(i) ?: FloatArray(0)

            FloatArray(currentContext.size + currentInput.size).apply {
                System.arraycopy(currentContext, 0, this, 0, currentContext.size)
                System.arraycopy(currentInput, 0, this, currentContext.size, currentInput.size)
            }
        }

        OnnxTensor.createTensor(env, combinedInput).use { inputTensor ->
            OnnxTensor.createTensor(env, state).use { stateTensor ->
                val inputs = mapOf(
                    "input" to inputTensor,
                    "state" to stateTensor,
                    "sr" to srTensor
                )

                session.run(inputs).use { ortOutputs ->
                    val output = (ortOutputs[0].value as? Array<FloatArray>)?.firstOrNull()
                        ?: throw IllegalStateException("Model output is null or invalid.")

                    state = (ortOutputs[1].value as? Array<Array<FloatArray>>)
                        ?: throw IllegalStateException("Model state output is null or invalid.")

                    for (i in combinedInput.indices) {
                        System.arraycopy(
                            combinedInput[i],
                            combinedInput[i].size - CONTEXT_SIZE,
                            context[i],
                            0,
                            CONTEXT_SIZE
                        )
                    }

                    lastBatchSize = batchSize

                    output
                }
            }
        }
    }

    override fun reset(batchSize: Int) = runCatching {
        require(batchSize <= BATCH_SIZE) { "Batch size exceeds maximum allowed size: $BATCH_SIZE" }

        state = Array(2) { Array(1) { FloatArray(128) } }

        context = Array(batchSize) { FloatArray(CONTEXT_SIZE) }

        lastBatchSize = batchSize
    }

    override fun close() {
        srTensor.close()
        session.close()
    }
}