package com.example.antyspamer;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GuardianAdapter.OnGuardianDeleteListener {

    private Button startBtn, settingsBtn;
    private ImageButton historyBtn;
    private FloatingActionButton addGuardianFab;
    private TextView statusText, speechText, warningText;
    private View micIndicator;
    private RecyclerView guardiansRecyclerView;
    private GuardianAdapter guardianAdapter;
    private List<Guardian> guardianList;
    
    private SharedPreferences sharedPreferences;
    private DatabaseHelper dbHelper;

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final String CHANNEL_ID = "YouthGuardAlerts";
    private boolean isProtectionActive = false;

    private TextInputEditText currentDialogNameInput;
    private TextInputEditText currentDialogPhoneInput;

    private final ActivityResultLauncher<Intent> contactPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri contactUri = result.getData().getData();
                    queryContactData(contactUri, true);
                }
            }
    );

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

    private final BroadcastReceiver speechReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.hasExtra("text")) {
                displayRecognizedText(intent.getStringExtra("text"), getActiveKeywords());
            }
        }
    };

    private final BroadcastReceiver confirmationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "POTWIERDZONO".equals(intent.getStringExtra("status"))) {
                playAlarmSound();
                showTopNotification("ZAGROŻENIE POTWIERDZONE!", "Twoje dziecko może potrzebować pomocy!");
                if (warningText != null) {
                    warningText.setText("!!! OPIEKUN POTWIERDZIŁ ZAGROŻENIE !!!");
                    warningText.setTextColor(Color.RED);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("SpamPrefs", MODE_PRIVATE);
        
        createNotificationChannel();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        startBtn = findViewById(R.id.startBtn);
        addGuardianFab = findViewById(R.id.addGuardianFab);
        historyBtn = findViewById(R.id.historyBtn);
        settingsBtn = findViewById(R.id.settingsBtn);
        statusText = findViewById(R.id.statusText);
        speechText = findViewById(R.id.speechText);
        warningText = findViewById(R.id.warningText);
        micIndicator = findViewById(R.id.micIndicator);
        guardiansRecyclerView = findViewById(R.id.guardiansRecyclerView);

        guardiansRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadGuardians();

        addGuardianFab.setOnClickListener(v -> showAddGuardianOptions());
        historyBtn.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        settingsBtn.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        startBtn.setOnClickListener(v -> toggleProtection());

        IntentFilter speechFilter = new IntentFilter("com.example.antyspamer.SPEECH_RESULT");
        IntentFilter confirmFilter = new IntentFilter("com.example.antyspamer.SMS_CONFIRMATION");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(speechReceiver, speechFilter, Context.RECEIVER_EXPORTED);
            registerReceiver(confirmationReceiver, confirmFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(speechReceiver, speechFilter);
            registerReceiver(confirmationReceiver, confirmFilter);
        }
        updateUIState();
    }

    private void loadGuardians() {
        guardianList = dbHelper.getAllGuardians();
        guardianAdapter = new GuardianAdapter(guardianList, this);
        guardiansRecyclerView.setAdapter(guardianAdapter);
    }

    @Override
    public void onDelete(Guardian guardian) {
        dbHelper.deleteGuardian(guardian.getId());
        loadGuardians();
    }

    private void showAddGuardianOptions() {
        new AlertDialog.Builder(this)
            .setTitle("Dodaj Opiekuna")
            .setItems(new CharSequence[]{"Wybierz z kontaktów", "Dodaj ręcznie"}, (dialog, which) -> {
                if (which == 0) {
                    pickContact();
                } else {
                    showAddGuardianDialog();
                }
            })
            .show();
    }

    private void showAddGuardianDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_guardian, null);
        currentDialogNameInput = view.findViewById(R.id.dialogGuardianName);
        currentDialogPhoneInput = view.findViewById(R.id.dialogGuardianPhone);

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Dodaj", (d, w) -> {
                String n = currentDialogNameInput.getText().toString().trim();
                String p = currentDialogPhoneInput.getText().toString().trim();
                if (!n.isEmpty() && !p.isEmpty()) {
                    dbHelper.addGuardian(n, p);
                    loadGuardians();
                } else {
                    Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Anuluj", null)
            .show();
    }

    private void pickContact() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 102);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            contactPickerLauncher.launch(intent);
        }
    }

    private void queryContactData(Uri contactUri, boolean autoAdd) {
        String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
        try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String number = cursor.getString(0);
                String name = cursor.getString(1);

                if (autoAdd) {
                    dbHelper.addGuardian(name, number);
                    loadGuardians();
                    Toast.makeText(this, "Dodano opiekuna: " + name, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleProtection() {
        if (isProtectionActive) {
            stopService(new Intent(this, MonitoringService.class));
        } else {
            checkPermissions();
        }
        isProtectionActive = !isProtectionActive;
        updateUIState();
    }

    private void updateUIState() {
        if (isProtectionActive) {
            startBtn.setText("Wyłącz ochronę");
            statusText.setText("Ochrona aktywna");
            statusText.setTextColor(Color.GREEN);
            if (micIndicator != null && micIndicator.getBackground() != null) {
                micIndicator.getBackground().setTint(Color.GREEN);
            }
        } else {
            startBtn.setText("Włącz ochronę");
            statusText.setText("Ochrona wyłączona");
            statusText.setTextColor(Color.RED);
            if (micIndicator != null && micIndicator.getBackground() != null) {
                micIndicator.getBackground().setTint(Color.RED);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(CHANNEL_ID, "Alerty YouthGuard", NotificationManager.IMPORTANCE_HIGH);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private void showTopNotification(String t, String c) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_alert)
                    .setContentTitle(t)
                    .setContentText(c)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);
            NotificationManagerCompat.from(this).notify(1, b.build());
        }
    }

    private void playAlarmSound() {
        try {
            Uri n = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), n);
            if (r != null) r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String> getActiveKeywords() {
        ArrayList<String> list = new ArrayList<>();
        if (sharedPreferences.getBoolean("detect_drugs", true)) list.addAll(Arrays.asList(drugKeywords));
        if (sharedPreferences.getBoolean("detect_self_harm", true)) list.addAll(Arrays.asList(selfHarmKeywords));
        if (sharedPreferences.getBoolean("detect_bullying", true)) list.addAll(Arrays.asList(bullyingKeywords));
        if (sharedPreferences.getBoolean("detect_alcohol", true)) list.addAll(Arrays.asList(alcoholKeywords));
        return list;
    }

    private void displayRecognizedText(String text, ArrayList<String> keywords) {
        SpannableString spannable = new SpannableString(text);
        for (String k : keywords) {
            int i = text.toLowerCase().indexOf(k.toLowerCase());
            while (i >= 0) {
                spannable.setSpan(new ForegroundColorSpan(Color.RED), i, i + k.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                i = text.toLowerCase().indexOf(k.toLowerCase(), i + 1);
            }
        }
        speechText.setText(spannable);
    }

    private void checkPermissions() {
        ArrayList<String> toRequest = new ArrayList<>();
        toRequest.add(Manifest.permission.RECORD_AUDIO);
        toRequest.add(Manifest.permission.SEND_SMS);
        toRequest.add(Manifest.permission.RECEIVE_SMS);
        toRequest.add(Manifest.permission.READ_PHONE_STATE);
        toRequest.add(Manifest.permission.READ_CALL_LOG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            toRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        ArrayList<String> needed = new ArrayList<>();
        for (String p : toRequest) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            startMonitoringService();
        }
    }

    private void startMonitoringService() {
        ContextCompat.startForegroundService(this, new Intent(this, MonitoringService.class));
    }

    @Override
    public void onRequestPermissionsResult(int rc, @NonNull String[] ps, @NonNull int[] gr) {
        super.onRequestPermissionsResult(rc, ps, gr);
        if (rc == PERMISSION_REQUEST_CODE) {
            boolean all = true;
            for (int r : gr) if (r != PackageManager.PERMISSION_GRANTED) all = false;
            if (all) startMonitoringService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { 
            unregisterReceiver(speechReceiver); 
            unregisterReceiver(confirmationReceiver); 
        } catch (Exception e) {}
    }
}
