package com.zain.h59heartguard;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.io.IOException;
import java.util.Locale;

public final class AlertVoicePlayer {
    private static final String UTTERANCE_ID = "h59_alert";
    private static final int TONE_DURATION_MS = 400;

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final TextToSpeech tts;
    private boolean ttsReady;
    private MediaPlayer mediaPlayer;
    private ToneGenerator tone;
    private Runnable onComplete;

    public AlertVoicePlayer(Context context) {
        this.context = context.getApplicationContext();
        tts = new TextToSpeech(this.context, status -> main.post(() -> onTtsReady(status)));
    }

    public static String message(Context context, boolean high, boolean medicationWording) {
        if (!high) return context.getString(R.string.voice_low);
        return context.getString(medicationWording ? R.string.voice_high_medication : R.string.voice_high_neutral);
    }

    public void play(String customAudioPath, String message, Runnable onDone) {
        stop();
        onComplete = onDone;
        if (customAudioPath != null) {
            playFile(customAudioPath, message);
        } else {
            speak(message);
        }
    }

    public void stop() {
        onComplete = null;
        tts.stop();
        releaseMedia();
    }

    public void release() {
        stop();
        tts.shutdown();
        if (tone != null) {
            tone.release();
            tone = null;
        }
    }

    private void onTtsReady(int status) {
        if (status != TextToSpeech.SUCCESS) return;
        int result = tts.setLanguage(Locale.forLanguageTag("ar"));
        ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
        tts.setAudioAttributes(attributes());
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) { }
            @Override public void onDone(String utteranceId) { main.post(AlertVoicePlayer.this::complete); }
            @Override public void onError(String utteranceId) { main.post(AlertVoicePlayer.this::complete); }
        });
    }

    private void playFile(String path, String fallbackMessage) {
        MediaPlayer player = new MediaPlayer();
        mediaPlayer = player;
        try {
            player.setAudioAttributes(attributes());
            player.setDataSource(path);
            player.setOnPreparedListener(MediaPlayer::start);
            player.setOnCompletionListener(mp -> {
                releaseMedia();
                complete();
            });
            player.setOnErrorListener((mp, what, extra) -> {
                releaseMedia();
                speak(fallbackMessage);
                return true;
            });
            player.prepareAsync();
        } catch (IOException | IllegalStateException e) {
            releaseMedia();
            speak(fallbackMessage);
        }
    }

    private void speak(String message) {
        if (!ttsReady) {
            playTone();
            complete();
            return;
        }
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
    }

    private void playTone() {
        if (tone == null) tone = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, TONE_DURATION_MS);
    }

    private void complete() {
        Runnable done = onComplete;
        onComplete = null;
        if (done != null) done.run();
    }

    private void releaseMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private static AudioAttributes attributes() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
    }
}
