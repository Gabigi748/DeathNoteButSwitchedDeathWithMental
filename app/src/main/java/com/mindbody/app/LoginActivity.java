package com.mindbody.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mindbody.app.network.ApiService;
import com.mindbody.app.network.RetrofitClient;
import com.mindbody.app.util.SharedPrefManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvForgotPassword, tvRegister;
    private ProgressBar progressBar;
    private SharedPrefManager prefManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefManager = new SharedPrefManager(this);

        // If already logged in, go to main
        if (prefManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        apiService = RetrofitClient.getInstance(this).getApiService();

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvRegister = findViewById(R.id.tv_register);
        progressBar = findViewById(R.id.progress_bar);

        btnLogin.setOnClickListener(v -> performLogin());

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void performLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "請填寫所有欄位", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        apiService.login(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    String token = (String) data.get("token");
                    prefManager.saveToken(token);

                    // Save user info if available
                    if (data.containsKey("user")) {
                        Map<String, Object> user = (Map<String, Object>) data.get("user");
                        if (user != null && user.containsKey("nickname")) {
                            prefManager.saveNickname((String) user.get("nickname"));
                        }
                        if (user != null && user.containsKey("email")) {
                            prefManager.saveEmail((String) user.get("email"));
                        }
                    }

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, R.string.error_login, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}
