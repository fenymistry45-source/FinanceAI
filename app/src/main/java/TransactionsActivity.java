package com.example.financeai;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TransactionsActivity extends AppCompatActivity {

    TextView tvBack;
    TextView tvMonth;
    TextView tvMonthIncome;
    TextView tvMonthExpense;

    LinearLayout allTransactionContainer;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_transactions);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvBack = findViewById(R.id.tvBack);
        tvMonth = findViewById(R.id.tvMonth);

        tvMonthIncome =
                findViewById(R.id.tvMonthIncome);

        tvMonthExpense =
                findViewById(R.id.tvMonthExpense);

        allTransactionContainer =
                findViewById(
                        R.id.allTransactionContainer
                );

        // Back
        tvBack.setOnClickListener(v -> finish());

        // Current month
        Calendar calendar = Calendar.getInstance();

        String monthName =
                new SimpleDateFormat(
                        "MMMM yyyy",
                        Locale.getDefault()
                ).format(
                        calendar.getTime()
                );

        tvMonth.setText(monthName);

        // Load current month transactions
        loadMonthlyTransactions();
    }


    // =====================================================
    // LOAD CURRENT MONTH TRANSACTIONS
    // =====================================================

    private void loadMonthlyTransactions() {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String userId =
                mAuth.getCurrentUser().getUid();

        Calendar startCalendar =
                Calendar.getInstance();

        startCalendar.set(
                Calendar.DAY_OF_MONTH,
                1
        );

        startCalendar.set(
                Calendar.HOUR_OF_DAY,
                0
        );

        startCalendar.set(
                Calendar.MINUTE,
                0
        );

        startCalendar.set(
                Calendar.SECOND,
                0
        );

        startCalendar.set(
                Calendar.MILLISECOND,
                0
        );


        Calendar endCalendar =
                Calendar.getInstance();

        endCalendar.set(
                Calendar.DAY_OF_MONTH,
                endCalendar.getActualMaximum(
                        Calendar.DAY_OF_MONTH
                )
        );

        endCalendar.set(
                Calendar.HOUR_OF_DAY,
                23
        );

        endCalendar.set(
                Calendar.MINUTE,
                59
        );

        endCalendar.set(
                Calendar.SECOND,
                59
        );

        endCalendar.set(
                Calendar.MILLISECOND,
                999
        );


        long startTime =
                startCalendar.getTimeInMillis();

        long endTime =
                endCalendar.getTimeInMillis();


        db.collection("transactions")
                .whereEqualTo(
                        "userId",
                        userId
                )
                .whereGreaterThanOrEqualTo(
                        "createdAt",
                        startTime
                )
                .whereLessThanOrEqualTo(
                        "createdAt",
                        endTime
                )
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING
                )
                .get()
                .addOnSuccessListener(
                        documents -> {

                            double income = 0;
                            double expense = 0;

                            allTransactionContainer
                                    .removeAllViews();


                            // =====================================
                            // NO TRANSACTIONS
                            // =====================================

                            if (documents.isEmpty()) {

                                tvMonthIncome.setText("₹0");
                                tvMonthExpense.setText("₹0");

                                showEmptyMessage();

                                return;
                            }


                            // =====================================
                            // LOOP ALL TRANSACTIONS
                            // =====================================

                            for (
                                    QueryDocumentSnapshot document :
                                    documents
                            ) {

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
                                                    amountObject
                                                            .toString()
                                            );

                                } catch (Exception e) {

                                    continue;
                                }


                                if ("income".equals(type)) {

                                    income += amount;

                                } else if (
                                        "expense".equals(type)
                                ) {

                                    expense += amount;
                                }


                                addTransactionCard(
                                        document
                                );
                            }


                            // =====================================
                            // MONTH SUMMARY
                            // =====================================

                            tvMonthIncome.setText(
                                    formatMoney(income)
                            );

                            tvMonthExpense.setText(
                                    formatMoney(expense)
                            );
                        }
                )
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            TransactionsActivity.this,
                            "Unable to load transactions: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }


    // =====================================================
    // EMPTY MESSAGE
    // =====================================================

    private void showEmptyMessage() {

        TextView emptyText =
                new TextView(this);

        emptyText.setText(
                "No transactions this month"
        );

        emptyText.setTextSize(15);

        emptyText.setTextColor(
                Color.rgb(120, 120, 120)
        );

        emptyText.setGravity(
                Gravity.CENTER
        );

        emptyText.setPadding(
                20,
                50,
                20,
                50
        );

        allTransactionContainer.addView(
                emptyText
        );
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


        // Card

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


        GradientDrawable cardBackground =
                new GradientDrawable();

        cardBackground.setColor(
                Color.WHITE
        );

        cardBackground.setCornerRadius(
                22
        );

        card.setBackground(
                cardBackground
        );


        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        82
                );

        cardParams.setMargins(
                0,
                0,
                0,
                10
        );

        card.setLayoutParams(
                cardParams
        );


        // Icon

        TextView icon =
                new TextView(this);

        icon.setText("•••");

        icon.setGravity(
                Gravity.CENTER
        );

        icon.setTextSize(18);

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
                18
        );

        icon.setBackground(
                iconBackground
        );


        card.addView(
                icon,
                new LinearLayout.LayoutParams(
                        48,
                        48
                )
        );


        // Middle

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

        if (title != null &&
                !title.isEmpty()) {

            titleText.setText(title);

        } else if (category != null) {

            titleText.setText(category);

        } else {

            titleText.setText("Transaction");
        }

        titleText.setTextSize(16);

        titleText.setTextColor(
                Color.rgb(20, 20, 20)
        );

        titleText.setTypeface(
                null,
                Typeface.BOLD
        );


        TextView dateText =
                new TextView(this);

        dateText.setText(
                date != null ? date : ""
        );

        dateText.setTextSize(12);

        dateText.setTextColor(
                Color.rgb(140, 150, 165)
        );

        dateText.setPadding(
                0,
                3,
                0,
                0
        );


        middle.addView(titleText);
        middle.addView(dateText);


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


        // Right

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

        amountText.setText(
                (isIncome ? "+₹" : "-₹")
                        + formatNumber(amount)
        );

        amountText.setTextSize(16);

        amountText.setTypeface(
                null,
                Typeface.BOLD
        );

        amountText.setTextColor(
                isIncome
                        ? Color.rgb(0, 158, 83)
                        : Color.rgb(244, 67, 54)
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


        right.addView(amountText);
        right.addView(categoryText);

        card.addView(right);


        allTransactionContainer.addView(
                card
        );
    }


    // =====================================================
    // FORMAT MONEY
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
}