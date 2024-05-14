package com.example.cameraxtrial;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Toast;

import java.util.Locale;

public class Home extends AppCompatActivity {
    private TextToSpeech myTTS;
    private Handler handler; // handler for thread communication
    private int CAM_CODE = 875; // activity result code
    private Context context;//global context instance
    private GestureDetector detector;//gesture detector for handling taps

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        context = this;
        handler = new Handler();
        new Thread(this::initializeTextToSpeech).start();//thread to handle TTS events
        addGestures();//init Gestures


    }
    @SuppressLint("ClickableViewAccessibility")
    private void addGestures() {
        //init gesture detector
        detector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                return true;//should return true to listen for event
            }

        });
        detector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                //launch camera
                startActivity(new Intent(context, MainActivity.class));
                return true;//should return true to listen for event
            }

            @Override
            public boolean onDoubleTap(MotionEvent motionEvent) {
                return true;
                //should return true to listen for event
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent motionEvent) {
                //add action on double tap gesture
                if (!myTTS.isSpeaking())//if TTF is free
                    speak("Document reading action coming soon");
                return true;
            }
        });

        //attaching gestures to parent layout of screen
        findViewById(R.id.mainHome).setOnTouchListener((view, motionEvent) -> {
            detector.onTouchEvent(motionEvent);
            return true;
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("Resume");
        if (myTTS != null && !myTTS.isSpeaking())
            speak("I m ready, tap on screen for note detection");
    }
    
    private void initializeTextToSpeech() {
        myTTS = new TextToSpeech(this, i -> {
            if (myTTS.getEngines().size() == 0) {
                handler.post(() -> {
                    Toast.makeText(Home.this, "There isn't TTS engine on your device",
                            Toast.LENGTH_LONG).show();
                });
            } else {
                myTTS.setLanguage(Locale.getDefault());
                myTTS.setSpeechRate(-10);
                speak("I m ready, tap on screen for note detection ");
            }
        });
    }

    private void speak(String message) {
        if (Build.VERSION.SDK_INT >= 24) {
            myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);

        } else {
            if (!myTTS.isSpeaking())
                myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
    }
}