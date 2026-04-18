package com.example.antyspamer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText editName;
    private CheckBox checkDrugs, checkSelfHarm, checkBullying, checkAlcohol;
    private Button backBtn;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        editName = findViewById(R.id.editName);
        checkDrugs = findViewById(R.id.checkDrugs);
        checkSelfHarm = findViewById(R.id.checkSelfHarm);
        checkBullying = findViewById(R.id.checkBullying);
        checkAlcohol = findViewById(R.id.checkAlcohol);
        backBtn = findViewById(R.id.backBtn);

        prefs = getSharedPreferences("SpamPrefs", MODE_PRIVATE);

        // Załaduj aktualne ustawienia
        editName.setText(prefs.getString("user_name", ""));
        checkDrugs.setChecked(prefs.getBoolean("detect_drugs", true));
        checkSelfHarm.setChecked(prefs.getBoolean("detect_self_harm", true));
        checkBullying.setChecked(prefs.getBoolean("detect_bullying", true));
        checkAlcohol.setChecked(prefs.getBoolean("detect_alcohol", true));

        backBtn.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();

            prefs.edit()
                    .putString("user_name", name)
                    .putBoolean("detect_drugs", checkDrugs.isChecked())
                    .putBoolean("detect_self_harm", checkSelfHarm.isChecked())
                    .putBoolean("detect_bullying", checkBullying.isChecked())
                    .putBoolean("detect_alcohol", checkAlcohol.isChecked())
                    .apply();

            Toast.makeText(this, "Ustawienia zapisane", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
