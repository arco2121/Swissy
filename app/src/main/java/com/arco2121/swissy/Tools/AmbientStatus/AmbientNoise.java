package com.arco2121.swissy.Tools.AmbientStatus;

import android.Manifest;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresPermission;

import com.arco2121.swissy.Managers.SettingsManager;
import com.arco2121.swissy.Tools.ToolListener;
import com.arco2121.swissy.Tools.ToolStructure;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AmbientNoise implements ToolStructure {

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord audioRecord;
    private int bufferSize;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private ExecutorService executor;
    private AmbientStatusListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    public static final String[] permissionList = {
            Manifest.permission.RECORD_AUDIO
    };
    private static final double ALPHA = 0.3;
    private double smoothedDecibels = 0;
    private boolean isFirstReading = true;

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public AmbientNoise(Context c) throws Exception {

        bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
        );

        if (bufferSize <= 0) {
            throw new Exception("Cannot create Noise Listener");
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
        );

        startSensors(c);
    }

    @Override
    public void setListener(ToolListener listener) {
        this.listener = (AmbientStatusListener) listener;
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Override
    public void startSensors(Context c) {
        if (SettingsManager.getPropreties(c).getBoolean("energysafer", false)) {
            return;
        }

        if (isRecording.get()) return;

        isRecording.set(true);
        if(audioRecord == null) {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
            );
        }
        audioRecord.startRecording();

        executor = Executors.newSingleThreadExecutor();
        executor.execute(this::processAudio);
    }

    @Override
    public void stopSensors() {
        isRecording.set(false);

        if (audioRecord != null &&
                audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.stop();
        }

        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
    }

    private void processAudio() {
        short[] buffer = new short[bufferSize / 2];
        while (isRecording.get() && !Thread.currentThread().isInterrupted()) {
            int read = audioRecord.read(buffer, 0, buffer.length);
            if (read <= 0) continue;

            double sum = 0;
            for (int i = 0; i < read; i++) {
                sum += buffer[i] * buffer[i];
            }

            double rms = Math.sqrt(sum / read);
            double decibels = 20 * Math.log10(rms + 1);

            if (isFirstReading) {
                smoothedDecibels = decibels;
                isFirstReading = false;
            } else {
                smoothedDecibels = ALPHA * decibels + (1 - ALPHA) * smoothedDecibels;
            }

            if (listener != null) {
                mainHandler.post(() -> listener.onNoise(smoothedDecibels));
            }
        }
    }
}