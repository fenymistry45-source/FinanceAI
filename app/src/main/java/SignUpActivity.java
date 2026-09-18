package com.example.financeai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignUpActivity extends AppCompatActivity {

    EditText etName, etEmail, etPassword, etConfirmPassword;
    Button btnSignup;
    TextView tvLogin;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);

        mAuth = FirebaseAuth.getInstance();

        btnSignup.setOnClickListener(v -> {

            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() ||
                    password.isEmpty() || confirmPassword.isEmpty()) {

                Toast.makeText(this,
                        "Please fill all fields",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this,
                        "Passwords do not match",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this,
                        "Password must be at least 6 characters",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {

                                UserProfileChangeRequest profileUpdates =
                                        new UserProfileChangeRequest.Builder()
                                                .setDisplayName(name)
                                                .build();

                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(profileTask -> {

                                            if (profileTask.isSuccessful()) {

                                                Toast.makeText(this,
                                                        "Account created successfully!",
                                                        Toast.LENGTH_SHORT).show();

                                                Intent intent = new Intent(
                                                        SignUpActivity.this,
                                                        DashboardActivity.class
                                                );

                                                startActivity(intent);
                                                finish();

                                            } else {

                                                Toast.makeText(this,
                                                        "Profile update failed",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {

                            Toast.makeText(this,
                                    "Signup failed: " +
                                            task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(
                    SignUpActivity.this,
                    MainActivity.class
            );

            startActivity(intent);
            finish();
        });
    }
}