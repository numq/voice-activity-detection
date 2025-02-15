package com.github.numq.voiceactivitydetection

/**
 * Represents the result of a speech detection process.
 *
 * @param fragments A list of detected speech fragments.
 * @param isLastFragmentComplete A boolean flag indicating whether the last fragment is complete.
 *                               If true, the last fragment can be used as is. If false, the fragment
 *                               is considered incomplete, and may need to be cached for further processing.
 */
data class DetectionResult(
    val fragments: List<ByteArray>,
    val isLastFragmentComplete: Boolean,
)