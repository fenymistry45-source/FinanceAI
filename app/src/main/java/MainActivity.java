package com.example.financeai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvSignup, tvForgotPassword;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        mAuth = FirebaseAuth.getInstance();

        // LOGIN
        btnLogin.setOnClickListener(v -> {

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this,
                        "Please enter email and password",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            Toast.makeText(this,
                                    "Login successful!",
                                    Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(
                                    MainActivity.this,
                                    DashboardActivity.class
                            );

                            startActivity(intent);
                            finish();

                        } else {

                            Toast.makeText(this,
                                    "Login failed: " +
                                            task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // SIGN UP
        tvSignup.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    SignUpActivity.class
            );
            startActivity(intent);
        });

        // FORGOT PASSWORD
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    ForgotPasswordActivity.class
            );
            startActivity(intent);
        });
    }
}