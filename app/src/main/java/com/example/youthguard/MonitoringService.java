package com.example.antyspamer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MonitoringService extends Service {
    private static final String CHANNEL_ID = "MonitoringServiceChannel";
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private SharedPreferences sharedPreferences;
    private DatabaseHelper dbHelper;
    private long lastSmsTime = 0;

    private final String[] drugKeywords = {
            "narkotyki", "dopalacze", "amfetamina", "mefedron", "kryształ", "piko", "trawa", 
            "zioło", "jaranie", "skun", "trawka", "piguły", "tabletki", "ekstazy", "mdma", 
            "lsd", "kwas", "grzyby", "heroina", "kompot", "kokaina", "koks", "feta"
    };
    private final String[] selfHarmKeywords = {
            "ciąć się", "tnę się", "chcę się zabić", "samobójstwo", "nie chce mi się żyć", 
            "zrobię sobie krzywdę", "żyletka", "krew", "blizny", "jestem beznadziejny", 
            "nikt mnie nie kocha", "lepiej by było gdybym umarł"
    };
    private final String[] bullyingKeywords = {
            "gnoją mnie", "śmieją się ze mnie", "dokuczają mi", "prześladują mnie", 
            "zastraszają mnie", "biją mnie", "szkoła to koszmar", "boję się iść do szkoły", 
            "wyśmiewają", "poniżają"
    };
    private final String[] alcoholKeywords = {
            "alkohol", "piwo", "wódka", "wino", "pijemy", "pijany", "najebany", "impreza",
            "melanż", "flaszka", "butelka", "browar", "jabole", "procenty"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("SpamPrefs", MODE_PRIVATE);
        dbHelper = new DatabaseHelper(this);
        createNotificationChannel();
        setupSpeechIntent();
        initSpeechRecognizer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("YouthGuard czuwa nad Twoim bezpieczeństwem")
                .setContentText("Aplikacja analizuje rozmowę w poszukiwaniu zagrożeń.")
                .setSmallIcon(android.R.drawable.ic_secure)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        startForeground(1, notification);
        startMonitoring();

        return START_STICKY;
    }

    private void startMonitoring() {
        if (speechRecognizer != null) {
            speechRecognizer.startListening(speechRecognizerIntent);
        }
    }

    private void setupSpeechIntent() {
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL");
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
    }

    private void initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    startMonitoring();
                } else {
                    Log.e("YouthGuardService", "Speech error: " + error);
                    new android.os.Handler(getMainLooper()).postDelayed(() -> startMonitoring(), 1000);
                }
            }
            @Override public void onResults(Bundle results) {
                processResults(results);
                startMonitoring();
            }
            @Override public void onPartialResults(Bundle partialResults) {
                processResults(partialResults);
            }
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void processResults(Bundle bundle) {
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            String text = matches.get(0);
            
            Intent intent = new Intent("com.example.antyspamer.SPEECH_RESULT");
            intent.putExtra("text", text);
            sendBroadcast(intent);

            checkForKeywords(text);
        }
    }

    private void checkForKeywords(String text) {
        ArrayList<String> activeKeywords = getActiveKeywords();
        String lowerText = text.toLowerCase();
        for (String keyword : activeKeywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                dbHelper.addAlert(keyword, text, "PENDING");
                sendAlertToGuardians(keyword, text);
                break;
            }
        }
    }

    private ArrayList<String> getActiveKeywords() {
        ArrayList<String> list = new ArrayList<>();
        if (sharedPreferences.getBoolean("detect_drugs", true)) {
            list.addAll(Arrays.asList(drugKeywords));
        }
        if (sharedPreferences.getBoolean("detect_self_harm", true)) {
            list.addAll(Arrays.asList(selfHarmKeywords));
        }
        if (sharedPreferences.getBoolean("detect_bullying", true)) {
            list.addAll(Arrays.asList(bullyingKeywords));
        }
        if (sharedPreferences.getBoolean("detect_alcohol", true)) {
            list.addAll(Arrays.asList(alcoholKeywords));
        }
        return list;
    }

    private void sendAlertToGuardians(String keyword, String fullText) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSmsTime < 120000) return;

        String userName = sharedPreferences.getString("user_name", "Twoje dziecko");
        String message = "ALERT YouthGuard! " + userName + " może potrzebować pomocy. Wykryto słowo: \"" + 
                         keyword + "\". Fragment rozmowy: \"" + fullText + "\". Odpisz POTWIERDZAM, aby uruchomić alarm na jego telefonie.";

        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                smsManager = this.getSystemService(SmsManager.class);
            } else {
                smsManager = SmsManager.getDefault();
            }

            List<Guardian> guardians = dbHelper.getAllGuardians(); // <-- NAPRAWIONA LINIJKA
            boolean sent = false;
            
            for (Guardian guardian : guardians) { // <-- NAPRAWIONA LINIJKA
                String num = guardian.getPhone();
                if (num != null && !num.isEmpty()) {
                    String cleanNum = num.replaceAll("\\s+", "");
                    ArrayList<String> parts = smsManager.divideMessage(message);
                    smsManager.sendMultipartTextMessage(cleanNum, null, parts, null, null);
                    sent = true;
                    Log.d("YouthGuardService", "Wysłano SMS do: " + cleanNum);
                }
            }
            if (sent) lastSmsTime = currentTime;
        } catch (Exception e) {
            Log.e("YouthGuardService", "Błąd wysyłania SMS", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "YouthGuard Monitoring Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
