package com.example.financeai;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    TextView tvWelcome;
    TextView tvMonthlyExpense;
    TextView tvBudgetPercent;
    TextView tvRemaining;

    TextView tvIncome;
    TextView tvExpense;
    TextView tvSavings;
    TextView tvViewAll;

    TextView tvNoTransactions;

    LinearLayout transactionContainer;

    ProgressBar progressBudget;

    BottomNavigationView bottomNavigation;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    private final List<QueryDocumentSnapshot> transactionList =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Views
        tvWelcome = findViewById(R.id.tvWelcome);

        tvMonthlyExpense =
                findViewById(R.id.tvMonthlyExpense);

        tvBudgetPercent =
                findViewById(R.id.tvBudgetPercent);

        tvRemaining =
                findViewById(R.id.tvRemaining);

        tvIncome =
                findViewById(R.id.tvIncome);

        tvExpense =
                findViewById(R.id.tvExpense);

        tvSavings =
                findViewById(R.id.tvSavings);

        transactionContainer =
                findViewById(R.id.transactionContainer);

        tvNoTransactions =
                findViewById(R.id.tvNoTransactions);

        progressBudget =
                findViewById(R.id.progressBudget);

        bottomNavigation =
                findViewById(R.id.bottomNavigation);

        // Bottom Navigation
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        tvViewAll = findViewById(R.id.tvViewAll);

        tvViewAll.setOnClickListener(v -> {

            Intent intent = new Intent(
                    DashboardActivity.this,
                    TransactionsActivity.class
            );

            startActivity(intent);
        });

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {

                return true;

            } else if (id == R.id.nav_reports) {

                Intent intent = new Intent(
                        DashboardActivity.this,
                        ReportsActivity.class
                );

                startActivity(intent);

                return true;

            } else if (id == R.id.nav_add) {

                Intent intent = new Intent(
                        DashboardActivity.this,
                        AddTransactionActivity.class
                );

                startActivity(intent);

                return true;

            } else if (id == R.id.nav_ai) {

                Intent intent = new Intent(
                        DashboardActivity.this,
                        AIAssistantActivity.class
                );

                startActivity(intent);

                return true;

            } else if (id == R.id.nav_profile) {

                Toast.makeText(
                        DashboardActivity.this,
                        "Profile coming soon",
                        Toast.LENGTH_SHORT
                ).show();

                return true;
            }

            return false;
        });

        // Load user
        loadUser();

        // Initial values
        resetDashboard();

        // Load transactions
        loadTransactions();
    }


    // =====================================================
    // LOAD USER
    // =====================================================

    private void loadUser() {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String name =
                mAuth.getCurrentUser().getDisplayName();

        if (name != null && !name.isEmpty()) {

            tvWelcome.setText(
                    "Welcome back, " + name + " 👋"
            );

        } else {

            tvWelcome.setText(
                    "Welcome back! 👋"
            );
        }
    }


    // =====================================================
    // RESET DASHBOARD
    // =====================================================

    private void resetDashboard() {

        tvMonthlyExpense.setText("₹0");
        tvIncome.setText("₹0");
        tvExpense.setText("₹0");
        tvSavings.setText("₹0");

        tvBudgetPercent.setText("0%");
        tvRemaining.setText("Remaining: ₹0");

        progressBudget.setProgress(0);

        transactionContainer.removeAllViews();

        TextView emptyText =
                new TextView(this);

        emptyText.setText(
                "No transactions yet"
        );

        emptyText.setTextSize(15);

        emptyText.setTextColor(
                Color.parseColor("#777777")
        );

        emptyText.setGravity(Gravity.CENTER);

        emptyText.setPadding(
                20,
                30,
                20,
                30
        );

        transactionContainer.addView(
                emptyText
        );
    }


    // =====================================================
    // LOAD TRANSACTIONS FROM FIRESTORE
    // =====================================================

    private void loadTransactions() {

        if (mAuth.getCurrentUser() == null) {

            resetDashboard();
            return;
        }

        String userId =
                mAuth.getCurrentUser().getUid();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(
                        queryDocumentSnapshots -> {

                            double totalIncome = 0;
                            double totalExpense = 0;

                            transactionList.clear();

                            for (
                                    QueryDocumentSnapshot document :
                                    queryDocumentSnapshots
                            ) {

                                transactionList.add(document);

                                Object amountObject =
                                        document.get("amount");

                                String type =
                                        document.getString("type");

                                if (amountObject == null ||
                                        type == null) {
                                    continue;
                                }

                                double amount;

                                try {

                                    amount =
                                            Double.parseDouble(
                                                    amountObject.toString()
                                            );

                                } catch (Exception e) {

                                    continue;
                                }

                                if ("income".equals(type)) {

                                    totalIncome += amount;

                                } else if ("expense".equals(type)) {

                                    totalExpense += amount;
                                }
                            }


                            // =========================================
                            // NO TRANSACTIONS
                            // =========================================

                            if (queryDocumentSnapshots.isEmpty()) {

                                resetDashboard();

                                return;
                            }


                            // =========================================
                            // SAVINGS
                            // =========================================

                            double savings =
                                    totalIncome - totalExpense;


                            // =========================================
                            // UPDATE VALUES
                            // =========================================

                            tvIncome.setText(
                                    formatMoney(totalIncome)
                            );

                            tvExpense.setText(
                                    formatMoney(totalExpense)
                            );

                            tvSavings.setText(
                                    formatMoney(savings)
                            );

                            tvMonthlyExpense.setText(
                                    formatMoney(totalExpense)
                            );


                            // =========================================
                            // BUDGET
                            //
                            // For now income is treated as
                            // available monthly budget.
                            // =========================================

                            double budget = totalIncome;

                            double percentage = 0;

                            if (budget > 0) {

                                percentage =
                                        (totalExpense / budget) * 100;

                            }

                            if (percentage > 100) {
                                percentage = 100;
                            }

                            int progress =
                                    (int) Math.round(percentage);

                            progressBudget.setProgress(
                                    progress
                            );

                            tvBudgetPercent.setText(
                                    progress + "%"
                            );


                            // =========================================
                            // REMAINING
                            // =========================================

                            double remaining =
                                    totalIncome - totalExpense;

                            if (remaining < 0) {
                                remaining = 0;
                            }

                            tvRemaining.setText(
                                    "Remaining: "
                                            + formatMoney(remaining)
                            );


                            // =========================================
                            // SHOW RECENT TRANSACTIONS
                            // =========================================

                            showRecentTransactions();
                        }
                )
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            DashboardActivity.this,
                            "Unable to load transactions: "
                                    + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }


    // =====================================================
    // SHOW RECENT TRANSACTIONS
    // =====================================================

    private void showRecentTransactions() {

        transactionContainer.removeAllViews();

        if (transactionList.isEmpty()) {

            resetDashboard();
            return;
        }


        // Sort by createdAt - newest first

        Collections.sort(
                transactionList,
                new Comparator<QueryDocumentSnapshot>() {

                    @Override
                    public int compare(
                            QueryDocumentSnapshot d1,
                            QueryDocumentSnapshot d2
                    ) {

                        Long time1 =
                                d1.getLong("createdAt");

                        Long time2 =
                                d2.getLong("createdAt");

                        if (time1 == null) {
                            time1 = 0L;
                        }

                        if (time2 == null) {
                            time2 = 0L;
                        }

                        return time2.compareTo(time1);
                    }
                }
        );


        // Show maximum 5 recent transactions

        int count =
                Math.min(
                        transactionList.size(),
                        5
                );


        for (int i = 0; i < count; i++) {

            QueryDocumentSnapshot document =
                    transactionList.get(i);

            addTransactionCard(document);
        }
    }


    // =====================================================
    // TRANSACTION CARD
    // =====================================================

    private void addTransactionCard(
            QueryDocumentSnapshot document
    ) {

        String type =
                document.getString("type");

        String category =
                document.getString("category");

        String title =
                document.getString("title");

        String date =
                document.getString("date");

        Object amountObject =
                document.get("amount");


        double amount = 0;

        if (amountObject != null) {

            try {

                amount =
                        Double.parseDouble(
                                amountObject.toString()
                        );

            } catch (Exception ignored) {
            }
        }


        boolean isIncome =
                "income".equals(type);


        // =========================================
        // OUTER CARD
        // =========================================

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.HORIZONTAL
        );

        card.setGravity(
                Gravity.CENTER_VERTICAL
        );

        card.setPadding(
                14,
                12,
                14,
                12
        );


        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.WHITE
        );

        background.setCornerRadius(
                20
        );

        card.setBackground(
                background
        );


        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        88
                );

        cardParams.setMargins(
                0,
                0,
                0,
                20

        );

        card.setLayoutParams(
                cardParams
        );


        // =========================================
        // ICON / DOT BOX
        // =========================================

        TextView icon =
                new TextView(this);

        icon.setText("•••");

        icon.setTextSize(18);

        icon.setGravity(
                Gravity.CENTER
        );

        icon.setTextColor(
                isIncome
                        ? Color.rgb(0, 158, 83)
                        : Color.rgb(244, 67, 54)
        );


        GradientDrawable iconBackground =
                new GradientDrawable();

        iconBackground.setColor(
                isIncome
                        ? Color.rgb(235, 248, 241)
                        : Color.rgb(255, 240, 240)
        );

        iconBackground.setCornerRadius(
                16
        );

        icon.setBackground(
                iconBackground
        );


        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(
                        48,
                        48
                );

        card.addView(
                icon,
                iconParams
        );


        // =========================================
        // MIDDLE CONTENT
        // =========================================

        LinearLayout middle =
                new LinearLayout(this);

        middle.setOrientation(
                LinearLayout.VERTICAL
        );

        middle.setPadding(
                13,
                0,
                5,
                0
        );


        TextView titleText =
                new TextView(this);

        String displayTitle;

        if (title != null &&
                !title.isEmpty()) {

            displayTitle = title;

        } else if (category != null &&
                !category.isEmpty()) {

            displayTitle = category;

        } else {

            displayTitle = "Transaction";
        }

        titleText.setText(
                displayTitle
        );

        titleText.setTextSize(16);

        titleText.setTextColor(
                Color.rgb(20, 20, 20)
        );

        titleText.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );


        TextView dateText =
                new TextView(this);

        dateText.setText(
                date != null && !date.isEmpty()
                        ? date
                        : ""
        );

        dateText.setTextSize(12);

        dateText.setTextColor(
                Color.rgb(140, 150, 165)
        );

        dateText.setPadding(
                0,
                5,
                0,
                0
        );


        middle.addView(
                titleText
        );

        middle.addView(
                dateText
        );


        LinearLayout.LayoutParams middleParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        card.addView(
                middle,
                middleParams
        );


        // =========================================
        // RIGHT SIDE
        // =========================================

        LinearLayout right =
                new LinearLayout(this);

        right.setOrientation(
                LinearLayout.VERTICAL
        );

        right.setGravity(
                Gravity.END
        );


        TextView amountText =
                new TextView(this);

        String sign =
                isIncome
                        ? "+₹"
                        : "-₹";

        amountText.setText(
                sign + formatNumber(amount)
        );

        amountText.setTextSize(16);

        amountText.setTextColor(
                isIncome
                        ? Color.rgb(0, 158, 83)
                        : Color.rgb(244, 67, 54)
        );

        amountText.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );


        TextView categoryText =
                new TextView(this);

        categoryText.setText(
                category != null
                        ? category
                        : ""
        );

        categoryText.setTextSize(11);

        categoryText.setTextColor(
                Color.rgb(145, 155, 170)
        );


        right.addView(
                amountText
        );

        right.addView(
                categoryText
        );


        card.addView(
                right
        );


        transactionContainer.addView(
                card
        );
    }


    // =====================================================
    // MONEY FORMAT
    // =====================================================

    private String formatMoney(double amount) {

        return "₹" + formatNumber(amount);
    }


    private String formatNumber(double amount) {

        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        new Locale("en", "IN")
                );

        formatter.setMaximumFractionDigits(0);

        return formatter.format(amount);
    }


    // =====================================================
    // REFRESH WHEN RETURNING
    // =====================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (mAuth != null) {

            loadTransactions();
        }
    }
}