package com.example.cameraxtrial;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cameraxtrial.ml.Model;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    PreviewView previewView;

    Button bTakePicture;
    private ImageCapture imageCapture;
    int wimage=180;
    int himage=180;
    TextView result,counter;


    // bone wala kam
    private TextToSpeech textToSpeech;

    //dabaune wala
    private Context context;
    private GestureDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bTakePicture = findViewById(R.id.bCapture);
        previewView = findViewById(R.id.previewView);
        result=findViewById(R.id.result);
        counter=findViewById(R.id.counter);

        textToSpeech = new TextToSpeech(this, onInitListener);

        bTakePicture.setOnClickListener(this);
        result.setText(R.string.avvnam);
        counter.setText("");
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, getExecutor());

        addGestures();

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
                capturePhoto();

                return true;//should return true to listen for event
            }

            @Override
            public boolean onDoubleTap(MotionEvent motionEvent) {
                return true;
                //should return true to listen for event
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent motionEvent) {
                return true;
            }
        });

        //attaching gestures to parent layout of screen
        findViewById(R.id.mainActivityParent).setOnTouchListener((view, motionEvent) -> {
            detector.onTouchEvent(motionEvent);
            return true;
        });
    }
    private TextToSpeech.OnInitListener onInitListener = i ->
            speak("Camera is open, place note and tap again for note detection");

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider) {

        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Preview preview = new Preview.Builder().build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(new Size(180, 180))
                .build();




        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }


    @SuppressLint("RestrictedApi")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bCapture: {
                if (textToSpeech != null && !textToSpeech.isSpeaking())
                    speak("This will take few seconds, please wait ");
                capturePhoto();
                break;
            }

        }
    }

    private void capturePhoto() {
        long timeStamp = System.currentTimeMillis();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timeStamp);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        Log.d("hihancy",String.valueOf(timeStamp));


        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                ).build(),
                getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(MainActivity.this,"Saving...",Toast.LENGTH_SHORT).show();
                        Uri saveimageuri= outputFileResults.getSavedUri();
                        // Convert the URI to a file path
                        String savedImagePath = getRealPathFromUri(saveimageuri);
                        if (savedImagePath != null) {
                            // Convert the file to a Bitmap
                            Bitmap bitmap = BitmapFactory.decodeFile(savedImagePath);
                            if (bitmap != null) {
                                bitmap = ThumbnailUtils.extractThumbnail(bitmap, 180, 180);
                                bitmap = Bitmap.createScaledBitmap(bitmap, 180, 180, true);
                                classifyImage(bitmap);

                            } else {
                                Toast.makeText(MainActivity.this, "Error while decoding", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "wrong file path", Toast.LENGTH_SHORT).show();
                        }

//                        loadAndDisplayImage(savedImageFile);

                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(MainActivity.this,"Error: "+exception.getMessage(),Toast.LENGTH_SHORT).show();


                    }
                });

    }

    private synchronized void speak(final String msg) {
        if (textToSpeech.getEngines().size() == 0) {
            Toast.makeText(MainActivity.this, "There isn't TTS engine on your device",
                    Toast.LENGTH_LONG).show();
            finish();
        } else {
            textToSpeech.setLanguage(Locale.getDefault());
            //slowing speech rate
            textToSpeech.setSpeechRate(-10);
            if (Build.VERSION.SDK_INT >= 24) {
                textToSpeech.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);

            } else {
                textToSpeech.speak(msg, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }
    private String getRealPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String filePath = cursor.getString(column_index);
            cursor.close();
            return filePath;
        } else {
            return null;
        }
    }




    private void classifyImage(Bitmap image) {
        try {


            Model model = Model.newInstance(getApplicationContext());


            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 180, 180, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect( 4 * wimage * himage * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[wimage * himage];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;
            //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
            for(int i = 0; i < wimage; i ++){
                for(int j = 0; j < himage; j++){
                    int val = intValues[pixel++]; // RGB
                   byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                   byteBuffer.putFloat((val & 0xFF) * (1.f / 1));

                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }


            String[] classes = {"1", "10","100","1000","2","20","5","50","500"};
            String ss=classes[maxPos];
            if (textToSpeech != null && !textToSpeech.isSpeaking())
                speak("this is a "+ ss + "Rupee note, Thank you");

            result.setText(classes[maxPos]);
            // Releases model resources if no longer used.
            model.close();
            // 12 sec timer to see the result and move to home activity again
            new CountDownTimer(5000, 1000) {
                public void onTick(long millisUntilFinished) {

                    long secondsRemaining = millisUntilFinished / 1000;
                    counter.setText(String.valueOf(secondsRemaining));
                }

                public void onFinish() {
                    Intent intent=new Intent(MainActivity.this,Home.class);
                    startActivity(intent);
                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
            // For example, show a toast message with the error
            Toast.makeText(getApplicationContext(), "Error running inference", Toast.LENGTH_SHORT);

        }
    }
}