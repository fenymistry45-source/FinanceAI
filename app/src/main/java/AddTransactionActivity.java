package com.example.financeai;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddTransactionActivity extends AppCompatActivity {

    EditText etAmount;
    EditText etTitle;
    EditText etNotes;
    EditText etDate;

    TextView tvExpense;
    TextView tvIncome;

    TextView category1;
    TextView category2;
    TextView category3;
    TextView category4;
    TextView category5;
    TextView category6;
    TextView category7;

    Button btnSaveTransaction;

    BottomNavigationView bottomNavigation;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    String transactionType = "expense";
    String selectedCategory = "Food";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_transaction);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // -----------------------------
        // Find Views
        // -----------------------------

        etAmount = findViewById(R.id.etAmount);
        etTitle = findViewById(R.id.etTitle);
        etNotes = findViewById(R.id.etNotes);
        etDate = findViewById(R.id.etDate);

        tvExpense = findViewById(R.id.tvExpense);
        tvIncome = findViewById(R.id.tvIncome);

        category1 = findViewById(R.id.category1);
        category2 = findViewById(R.id.category2);
        category3 = findViewById(R.id.category3);
        category4 = findViewById(R.id.category4);
        category5 = findViewById(R.id.category5);
        category6 = findViewById(R.id.category6);
        category7 = findViewById(R.id.category7);

        btnSaveTransaction = findViewById(R.id.btnSaveTransaction);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        // -----------------------------
        // Default Mode
        // -----------------------------

        setExpenseMode();

        // -----------------------------
        // Expense / Income Toggle
        // -----------------------------

        tvExpense.setOnClickListener(v -> {
            setExpenseMode();
        });

        tvIncome.setOnClickListener(v -> {
            setIncomeMode();
        });

        // -----------------------------
        // Category Selection
        // -----------------------------

        category1.setOnClickListener(v -> {
            selectCategory(category1);
        });

        category2.setOnClickListener(v -> {
            selectCategory(category2);
        });

        category3.setOnClickListener(v -> {
            selectCategory(category3);
        });

        category4.setOnClickListener(v -> {
            selectCategory(category4);
        });

        category5.setOnClickListener(v -> {
            selectCategory(category5);
        });

        category6.setOnClickListener(v -> {
            selectCategory(category6);
        });

        category7.setOnClickListener(v -> {
            selectCategory(category7);
        });

        // -----------------------------
        // Date Picker
        // -----------------------------

        etDate.setOnClickListener(v -> {
            openDatePicker();
        });

        // -----------------------------
        // Save Transaction
        // -----------------------------

        btnSaveTransaction.setOnClickListener(v -> {

            // First confirmation that button is clicked
            Toast.makeText(
                    AddTransactionActivity.this,
                    "Saving transaction...",
                    Toast.LENGTH_SHORT
            ).show();

            saveTransaction();
        });

        // -----------------------------
        // Bottom Navigation
        // -----------------------------

        bottomNavigation.setSelectedItemId(R.id.nav_add);

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            // Home
            if (id == R.id.nav_home) {

                startActivity(new Intent(
                        AddTransactionActivity.this,
                        DashboardActivity.class
                ));

                finish();

                return true;
            }

            // Reports
            else if (id == R.id.nav_reports) {

                startActivity(new Intent(
                        AddTransactionActivity.this,
                        ReportsActivity.class
                ));

                finish();

                return true;
            }

            // Add Transaction
            else if (id == R.id.nav_add) {

                return true;
            }

            // AI Assistant
            else if (id == R.id.nav_ai) {

                Toast.makeText(
                        AddTransactionActivity.this,
                        "AI Assistant coming soon",
                        Toast.LENGTH_SHORT
                ).show();

                return true;
            }

            // Profile
            else if (id == R.id.nav_profile) {

                Toast.makeText(
                        AddTransactionActivity.this,
                        "Profile coming soon",
                        Toast.LENGTH_SHORT
                ).show();

                return true;
            }

            return false;
        });
    }

    // =========================================================
    // EXPENSE MODE
    // =========================================================

    private void setExpenseMode() {

        transactionType = "expense";

        // Selected Expense
        tvExpense.setBackgroundResource(
                R.drawable.bg_transaction_selected
        );

        // Unselected Income
        tvIncome.setBackgroundResource(
                R.drawable.bg_transaction_unselected
        );

        // Text Colors
        tvExpense.setTextColor(
                getColor(android.R.color.holo_red_dark)
        );

        tvIncome.setTextColor(
                getColor(android.R.color.darker_gray)
        );

        // Expense Categories
        category1.setText("🍽️\nFood");
        category2.setText("✈️\nTravel");
        category3.setText("🛍️\nShopping");
        category4.setText("🧾\nBills");
        category5.setText("⛽\nPetrol");
        category6.setText("🛒\nGrocery");
        category7.setText("📦\nOther");

        selectedCategory = "Food";

        selectCategory(category1);
    }

    // =========================================================
    // INCOME MODE
    // =========================================================

    private void setIncomeMode() {

        transactionType = "income";

        // Selected Income
        tvIncome.setBackgroundResource(
                R.drawable.bg_transaction_selected
        );

        // Unselected Expense
        tvExpense.setBackgroundResource(
                R.drawable.bg_transaction_unselected
        );

        // Text Colors
        tvIncome.setTextColor(
                getColor(android.R.color.holo_green_dark)
        );

        tvExpense.setTextColor(
                getColor(android.R.color.darker_gray)
        );

        // Income Categories
        category1.setText("💼\nSalary");
        category2.setText("💻\nFreelance");
        category3.setText("🏢\nBusiness");
        category4.setText("📈\nInvestment");
        category5.setText("🎁\nGift");
        category6.setText("💰\nBonus");
        category7.setText("📦\nOther");

        selectedCategory = "Salary";

        selectCategory(category1);
    }

    // =========================================================
    // SELECT CATEGORY
    // =========================================================

    private void selectCategory(TextView selected) {

        // Reset all category backgrounds
        resetCategoryBackgrounds();

        // Highlight selected category
        selected.setBackgroundResource(
                R.drawable.bg_category_selected
        );

        String text = selected.getText().toString();

        if (text.contains("\n")) {

            selectedCategory =
                    text.substring(
                            text.indexOf("\n") + 1
                    );

        } else {

            selectedCategory = text;
        }
    }

    // =========================================================
    // RESET CATEGORY BACKGROUNDS
    // =========================================================

    private void resetCategoryBackgrounds() {

        category1.setBackgroundResource(
                R.drawable.bg_category
        );

        category2.setBackgroundResource(
                R.drawable.bg_category
        );

        category3.setBackgroundResource(
                R.drawable.bg_category
        );

        category4.setBackgroundResource(
                R.drawable.bg_category
        );

        category5.setBackgroundResource(
                R.drawable.bg_category
        );

        category6.setBackgroundResource(
                R.drawable.bg_category
        );

        category7.setBackgroundResource(
                R.drawable.bg_category
        );
    }

    // =========================================================
    // DATE PICKER
    // =========================================================

    private void openDatePicker() {

        Calendar calendar = Calendar.getInstance();

        int year =
                calendar.get(Calendar.YEAR);

        int month =
                calendar.get(Calendar.MONTH);

        int day =
                calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,
                        (view,
                         selectedYear,
                         selectedMonth,
                         selectedDay) -> {

                            String date =
                                    selectedDay +
                                            "/" +
                                            (selectedMonth + 1) +
                                            "/" +
                                            selectedYear;

                            etDate.setText(date);
                        },
                        year,
                        month,
                        day
                );

        datePickerDialog.show();
    }

    // =========================================================
    // SAVE TRANSACTION
    // =========================================================

    private void saveTransaction() {

        // -----------------------------
        // Get Form Data
        // -----------------------------

        String amountText =
                etAmount.getText()
                        .toString()
                        .trim();

        String title =
                etTitle.getText()
                        .toString()
                        .trim();

        String notes =
                etNotes.getText()
                        .toString()
                        .trim();

        String date =
                etDate.getText()
                        .toString()
                        .trim();

        // -----------------------------
        // Validate Amount
        // -----------------------------

        if (amountText.isEmpty()) {

            etAmount.setError(
                    "Enter amount"
            );

            etAmount.requestFocus();

            return;
        }

        // -----------------------------
        // Validate Title
        // -----------------------------

        if (title.isEmpty()) {

            etTitle.setError(
                    "Enter title"
            );

            etTitle.requestFocus();

            return;
        }

        // -----------------------------
        // Validate Date
        // -----------------------------

        if (date.isEmpty()) {

            etDate.setError(
                    "Select date"
            );

            etDate.requestFocus();

            return;
        }

        // -----------------------------
        // Check Login
        // -----------------------------

        if (mAuth.getCurrentUser() == null) {

            Toast.makeText(
                    AddTransactionActivity.this,
                    "Please login first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // -----------------------------
        // Convert Amount
        // -----------------------------

        double amount;

        try {

            amount =
                    Double.parseDouble(
                            amountText
                    );

        } catch (NumberFormatException e) {

            etAmount.setError(
                    "Enter valid amount"
            );

            etAmount.requestFocus();

            return;
        }

        // -----------------------------
        // Get User ID
        // -----------------------------

        String userId =
                mAuth.getCurrentUser()
                        .getUid();

        // -----------------------------
        // Create Transaction
        // -----------------------------

        Map<String, Object> transaction =
                new HashMap<>();

        transaction.put(
                "userId",
                userId
        );

        transaction.put(
                "type",
                transactionType
        );

        transaction.put(
                "amount",
                amount
        );

        transaction.put(
                "category",
                selectedCategory
        );

        transaction.put(
                "title",
                title
        );

        transaction.put(
                "notes",
                notes
        );

        transaction.put(
                "date",
                date
        );

        transaction.put(
                "createdAt",
                System.currentTimeMillis()
        );

        // -----------------------------
        // Save to Firestore
        // -----------------------------

        db.collection("transactions")
                .add(transaction)

                // SUCCESS
                .addOnSuccessListener(
                        documentReference -> {

                            Toast.makeText(
                                    AddTransactionActivity.this,
                                    transactionType.equals("expense")
                                            ? "Expense added successfully!"
                                            : "Income added successfully!",
                                    Toast.LENGTH_LONG
                            ).show();

                            // -----------------------------
                            // Clear Form
                            // -----------------------------

                            etAmount.setText("");
                            etTitle.setText("");
                            etNotes.setText("");
                            etDate.setText("");

                            // -----------------------------
                            // Reset to Expense
                            // -----------------------------

                            setExpenseMode();
                        }
                )

                // FAILURE
                .addOnFailureListener(
                        e -> {

                            Toast.makeText(
                                    AddTransactionActivity.this,
                                    "Failed to save transaction: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }
}
