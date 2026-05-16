package com.example.zell

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * VoiceRecorder — thin wrapper around [MediaRecorder] for recording voice notes.
 *
 * Usage:
 *   val recorder = VoiceRecorder(context)
 *   recorder.start()          // start capturing mic
 *   val file = recorder.stop()  // stop, returns the .m4a file (null if nothing recorded)
 *   recorder.cancel()         // stop and delete the temp file (user swiped to cancel)
 *
 * Output format: MPEG-4 container with AAC audio — plays on every Android and iOS device.
 * Files are written to [Context.cacheDir] and are the caller's responsibility to delete
 * after a successful upload.
 */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    /** True while [MediaRecorder] is actively capturing. */
    var isRecording: Boolean = false
        private set

    /**
     * Start recording from the device microphone.
     * Safe to call multiple times — stops any current session first.
     *
     * @throws Exception if [MediaRecorder] fails to prepare or start
     */
    fun start() {
        // Safety: stop any existing session before starting a new one
        stop()

        outputFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")

        @Suppress("DEPRECATION") // MediaRecorder(context) requires API 31+; old constructor works fine on API 24+
        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)   // 128 kbps — clear voice quality
            setAudioSamplingRate(44_100)        // 44.1 kHz
            setOutputFile(outputFile!!.absolutePath)
            prepare()
            start()
        }
        isRecording = true
        CrashlyticsLogger.i("VoiceRecorder", "Recording started → ${outputFile!!.name}")
    }

    /**
     * Stop recording and return the recorded file.
     * Returns null if no recording was in progress or if an error occurred.
     */
    fun stop(): File? {
        if (!isRecording) return null
        return try {
            recorder?.apply {
                stop()
                release()
            }
            isRecording = false
            recorder = null
            val file = outputFile
            outputFile = null
            CrashlyticsLogger.i("VoiceRecorder", "Recording stopped — ${file?.length()?.div(1024)} KB")
            file
        } catch (e: Exception) {
            // stop() can throw if recorder was in an error state (e.g., mic never captured anything)
            CrashlyticsLogger.w("VoiceRecorder", "Error stopping recorder: ${e.message}")
            release()
            null
        }
    }

    /**
     * Cancel the current recording — stops and deletes the temp file.
     * Call this when the user swipes to cancel.
     */
    fun cancel() {
        release()
        outputFile?.delete()
        outputFile = null
        CrashlyticsLogger.i("VoiceRecorder", "Recording cancelled and temp file deleted")
    }

    private fun release() {
        try {
            recorder?.apply { stop(); release() }
        } catch (_: Exception) { /* already stopped or never started */ }
        recorder = null
        isRecording = false
    }
}
