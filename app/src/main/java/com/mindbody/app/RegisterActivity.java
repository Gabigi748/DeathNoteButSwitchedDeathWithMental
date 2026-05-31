package com.mindbody.app;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mindbody.app.network.ApiService;
import com.mindbody.app.network.RetrofitClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword, etNickname;
    private Spinner spinnerGender;
    private MaterialButton btnBirthday, btnRegister;
    private CheckBox cbHypertension, cbDiabetes, cbHeart, cbAsthma;
    private CheckBox cbDepression, cbAnxiety, cbInsomnia, cbOtherDisease;
    private CheckBox cbDisclaimer;
    private ProgressBar progressBar;
    private ApiService apiService;
    private String selectedBirthday = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiService = RetrofitClient.getInstance(this).getApiService();

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etNickname = findViewById(R.id.et_nickname);
        spinnerGender = findViewById(R.id.spinner_gender);
        btnBirthday = findViewById(R.id.btn_birthday);
        btnRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progress_bar);
        cbDisclaimer = findViewById(R.id.cb_disclaimer);

        cbHypertension = findViewById(R.id.cb_hypertension);
        cbDiabetes = findViewById(R.id.cb_diabetes);
        cbHeart = findViewById(R.id.cb_heart);
        cbAsthma = findViewById(R.id.cb_asthma);
        cbDepression = findViewById(R.id.cb_depression);
        cbAnxiety = findViewById(R.id.cb_anxiety);
        cbInsomnia = findViewById(R.id.cb_insomnia);
        cbOtherDisease = findViewById(R.id.cb_other_disease);

        // Setup gender spinner
        String[] genders = {getString(R.string.gender_male), getString(R.string.gender_female), getString(R.string.gender_other)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genders);
        spinnerGender.setAdapter(adapter);

        // Birthday picker
        btnBirthday.setOnClickListener(v -> showDatePicker());

        // Register button
        btnRegister.setOnClickListener(v -> performRegister());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedBirthday = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    btnBirthday.setText(selectedBirthday);
                },
                cal.get(Calendar.YEAR) - 25,
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void performRegister() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String nickname = etNickname.getText() != null ? etNickname.getText().toString().trim() : "";
        String[] genderKeys = {"male", "female", "other"};
        String gender = genderKeys[spinnerGender.getSelectedItemPosition()];

        if (email.isEmpty() || password.isEmpty() || nickname.isEmpty()) {
            Toast.makeText(this, "請填寫所有必填欄位", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedBirthday.isEmpty()) {
            Toast.makeText(this, "請選擇出生日期", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!cbDisclaimer.isChecked()) {
            Toast.makeText(this, "請同意免責聲明", Toast.LENGTH_SHORT).show();
            return;
        }

        // Collect diseases
        List<String> diseases = new ArrayList<>();
        if (cbHypertension.isChecked()) diseases.add("高血壓");
        if (cbDiabetes.isChecked()) diseases.add("糖尿病");
        if (cbHeart.isChecked()) diseases.add("心臟病");
        if (cbAsthma.isChecked()) diseases.add("氣喘");
        if (cbDepression.isChecked()) diseases.add("憂鬱症");
        if (cbAnxiety.isChecked()) diseases.add("焦慮症");
        if (cbInsomnia.isChecked()) diseases.add("失眠");
        if (cbOtherDisease.isChecked()) diseases.add("其他");

        setLoading(true);

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("nickname", nickname);
        body.put("gender", gender);
        body.put("birthday", selectedBirthday);
        body.put("diseases", diseases);

        apiService.register(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, R.string.success_register, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, R.string.error_register, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
