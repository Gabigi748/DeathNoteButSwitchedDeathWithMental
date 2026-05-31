package com.mindbody.app;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        TextInputEditText etEmail = findViewById(R.id.et_email);
        MaterialButton btnSendReset = findViewById(R.id.btn_send_reset);

        btnSendReset.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            if (email.isEmpty()) {
                Toast.makeText(this, "請輸入電子郵件", Toast.LENGTH_SHORT).show();
                return;
            }
            // TODO: Implement forgot password API when backend supports it
            Toast.makeText(this, "重設密碼連結已發送至 " + email, Toast.LENGTH_LONG).show();
            finish();
        });
    }
}
