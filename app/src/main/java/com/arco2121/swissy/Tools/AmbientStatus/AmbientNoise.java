package com.arco2121.swissy.Tools.AmbientStatus;

import com.arco2121.swissy.Managers.SettingsManager;
import com.arco2121.swissy.Tools.ToolStructure;

import android.Manifest;
import android.content.Context;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Looper;
import android.os.Handler;

import androidx.annotation.RequiresPermission;


public class AmbientNoise implements ToolStructure {
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord audioRecord;
    private int bufferSize;
    private boolean isRecording = false;
    private Thread recordingThread;
    private AmbientStatusListener listener;
    public static final String[] permissionList = { Manifest.permission.RECORD_AUDIO };

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public AmbientNoise(Context c) throws Exception {
        bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
        );
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
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
    public void setListener(Object listener) {
        this.listener = (AmbientStatusListener) listener;
    }

    @Override
    public void startSensors(Context c) {
        if(SettingsManager.getPropreties(c).getBoolean("energysafer", false))
            return;
        isRecording = true;
        audioRecord.startRecording();
        recordingThread = new Thread(this::processAudio);
        recordingThread.start();
    }

    @Override
    public void stopSensors() {
        isRecording = false;
        if (audioRecord != null) {
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop();
            }
        }
        if (recordingThread != null) {
            try {
                recordingThread.join();
            } catch (InterruptedException ignored) {}
        }
    }

    private void processAudio() {
        short[] buffer = new short[bufferSize];

        while (isRecording) {
            int readSize = audioRecord.read(buffer, 0, bufferSize);

            if (readSize > 0) {
                double sum = 0;
                for (int i = 0; i < readSize; i++) {
                    sum += buffer[i] * buffer[i];
                }
                double rms = Math.sqrt(sum / readSize);
                double decibels = 20 * Math.log10(rms + 1);
                if(listener != null) listener.onNoise(decibels);
            }
        }
    }
}
