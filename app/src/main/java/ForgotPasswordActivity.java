package com.example.financeai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText etResetEmail;
    Button btnResetPassword;
    TextView tvBackToLogin;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Connect XML views
        etResetEmail = findViewById(R.id.etResetEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Send Reset Email
        btnResetPassword.setOnClickListener(v -> {

            String email = etResetEmail.getText().toString().trim();

            if (email.isEmpty()) {
                etResetEmail.setError("Enter your email");
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            Toast.makeText(
                                    ForgotPasswordActivity.this,
                                    "Password reset email sent!",
                                    Toast.LENGTH_LONG
                            ).show();

                        } else {

                            Toast.makeText(
                                    ForgotPasswordActivity.this,
                                    "Failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        // Back to Login
        tvBackToLogin.setOnClickListener(v -> {

            Intent intent = new Intent(
                    ForgotPasswordActivity.this,
                    MainActivity.class
            );

            startActivity(intent);
            finish();
        });
    }
}