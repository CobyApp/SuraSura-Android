package com.coby.surasura.data.model

/**
 * One [com.google.cloud.speech.v1.StreamingRecognizeResponse] may contain several
 * [com.google.cloud.speech.v1.StreamingRecognitionResult] entries (e.g. finalized + interim).
 * [isFinal] distinguishes stable text from in-progress hypotheses.
 */
data class SttStreamSegment(
    val text: String,
    val isFinal: Boolean
)
