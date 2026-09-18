package com.example.financeai;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportsActivity extends AppCompatActivity {

    // ==============================
    // FIREBASE
    // ==============================

    FirebaseAuth mAuth;
    FirebaseFirestore db;


    // ==============================
    // UI
    // ==============================

    Spinner spinnerMonth;

    TextView tvTotalIncome;
    TextView tvTotalExpense;
    TextView tvSavings;

    TextView tvNoCategory;
    TextView tvNoTransactions;

    LinearLayout categoryContainer;
    LinearLayout transactionContainer;

    BarChart barChart;
    PieChart pieChart;

    BottomNavigationView bottomNavigation;


    // ==============================
    // DATA
    // ==============================

    private final List<QueryDocumentSnapshot> allTransactions =
            new ArrayList<>();

    private final String[] monthNames = {
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December"
    };

    private int selectedMonth;
    private int selectedYear;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_reports);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        // Views
        spinnerMonth = findViewById(R.id.spinnerMonth);

        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvSavings = findViewById(R.id.tvSavings);

        tvNoCategory = findViewById(R.id.tvNoCategory);
        tvNoTransactions = findViewById(R.id.tvNoTransactions);

        categoryContainer =
                findViewById(R.id.categoryContainer);

        transactionContainer =
                findViewById(R.id.transactionContainer);

        barChart =
                findViewById(R.id.barChart);

        pieChart =
                findViewById(R.id.pieChart);

        bottomNavigation =
                findViewById(R.id.bottomNavigation);


        // Current month
        Calendar calendar = Calendar.getInstance();

        selectedMonth =
                calendar.get(Calendar.MONTH);

        selectedYear =
                calendar.get(Calendar.YEAR);


        setupMonthSpinner();

        setupCharts();

        setupBottomNavigation();

        resetReports();

        loadTransactions();
    }


    // =========================================================
    // MONTH SPINNER
    // =========================================================

    private void setupMonthSpinner() {

        List<String> months =
                new ArrayList<>();

        for (String month : monthNames) {
            months.add(month + " " + selectedYear);
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        months
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerMonth.setAdapter(adapter);

        spinnerMonth.setSelection(selectedMonth);

        spinnerMonth.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        selectedMonth = position;

                        updateReport();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );
    }


    // =========================================================
    // CHART SETUP
    // =========================================================

    private void setupCharts() {

        // ==============================
        // BAR CHART
        // ==============================

        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setFitBars(true);

        Description barDescription =
                new Description();

        barDescription.setText("");

        barChart.setDescription(
                barDescription
        );

        barChart.getAxisRight()
                .setEnabled(false);

        barChart.getAxisLeft()
                .setAxisMinimum(0);

        barChart.getXAxis()
                .setPosition(
                        com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                );

        barChart.getXAxis()
                .setGranularity(1f);

        barChart.getLegend()
                .setEnabled(true);


        // ==============================
        // PIE CHART
        // ==============================

        Description pieDescription =
                new Description();

        pieDescription.setText("");

        pieChart.setDescription(
                pieDescription
        );

        pieChart.setUsePercentValues(false);

        pieChart.setDrawHoleEnabled(true);

        pieChart.setHoleRadius(55f);

        pieChart.setTransparentCircleRadius(60f);

        pieChart.setCenterText(
                "Expenses"
        );

        pieChart.setCenterTextSize(16f);

        Legend legend =
                pieChart.getLegend();

        legend.setVerticalAlignment(
                Legend.LegendVerticalAlignment.BOTTOM
        );

        legend.setHorizontalAlignment(
                Legend.LegendHorizontalAlignment.CENTER
        );

        legend.setOrientation(
                Legend.LegendOrientation.HORIZONTAL
        );

        legend.setDrawInside(false);
    }


    // =========================================================
    // LOAD ALL USER TRANSACTIONS
    // =========================================================

    private void loadTransactions() {

        if (mAuth.getCurrentUser() == null) {

            resetReports();

            return;
        }

        String userId =
                mAuth.getCurrentUser().getUid();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(
                        queryDocumentSnapshots -> {

                            allTransactions.clear();

                            for (
                                    QueryDocumentSnapshot document :
                                    queryDocumentSnapshots
                            ) {

                                allTransactions.add(
                                        document
                                );
                            }

                            updateReport();
                        }
                )
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ReportsActivity.this,
                            "Unable to load reports: "
                                    + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();

                    resetReports();
                });
    }


    // =========================================================
    // UPDATE REPORT
    // =========================================================

    private void updateReport() {

        if (allTransactions.isEmpty()) {

            resetReports();

            return;
        }

        double totalIncome = 0;
        double totalExpense = 0;


        Map<String, Double> categoryMap =
                new HashMap<>();


        List<QueryDocumentSnapshot> monthTransactions =
                new ArrayList<>();


        // ==========================================
        // FILTER SELECTED MONTH
        // ==========================================

        for (
                QueryDocumentSnapshot document :
                allTransactions
        ) {

            String date =
                    document.getString("date");

            if (!isSelectedMonth(date)) {
                continue;
            }

            monthTransactions.add(
                    document
            );


            String type =
                    document.getString("type");

            double amount =
                    getAmount(document);


            if ("income".equals(type)) {

                totalIncome += amount;

            } else if ("expense".equals(type)) {

                totalExpense += amount;

                String category =
                        document.getString("category");

                if (category == null ||
                        category.isEmpty()) {

                    category = "Other";
                }

                if (categoryMap.containsKey(category)) {

                    categoryMap.put(
                            category,
                            categoryMap.get(category) + amount
                    );

                } else {

                    categoryMap.put(
                            category,
                            amount
                    );
                }
            }
        }


        // ==========================================
        // SAVINGS
        // ==========================================

        double savings =
                totalIncome - totalExpense;


        // ==========================================
        // UPDATE SUMMARY
        // ==========================================

        tvTotalIncome.setText(
                formatMoney(totalIncome)
        );

        tvTotalExpense.setText(
                formatMoney(totalExpense)
        );

        tvSavings.setText(
                formatMoney(savings)
        );


        // ==========================================
        // UPDATE BAR CHART
        // ==========================================

        updateBarChart(
                totalIncome,
                totalExpense
        );


        // ==========================================
        // UPDATE PIE CHART
        // ==========================================

        updatePieChart(
                categoryMap
        );


        // ==========================================
        // CATEGORY LIST
        // ==========================================

        updateCategoryList(
                categoryMap,
                totalExpense
        );


        // ==========================================
        // TRANSACTIONS
        // ==========================================

        updateTransactions(
                monthTransactions
        );
    }


    // =========================================================
    // CHECK SELECTED MONTH
    // =========================================================

    private boolean isSelectedMonth(
            String date
    ) {

        if (date == null ||
                date.isEmpty()) {

            return false;
        }

        try {

            String[] parts =
                    date.split("/");

            if (parts.length != 3) {
                return false;
            }

            int day =
                    Integer.parseInt(parts[0]);

            int month =
                    Integer.parseInt(parts[1]) - 1;

            int year =
                    Integer.parseInt(parts[2]);


            return month == selectedMonth
                    && year == selectedYear;

        } catch (Exception e) {

            return false;
        }
    }


    // =========================================================
    // BAR CHART
    // =========================================================

    private void updateBarChart(
            double income,
            double expense
    ) {

        ArrayList<BarEntry> entries =
                new ArrayList<>();

        entries.add(
                new BarEntry(
                        0,
                        (float) income
                )
        );

        entries.add(
                new BarEntry(
                        1,
                        (float) expense
                )
        );


        BarDataSet dataSet =
                new BarDataSet(
                        entries,
                        "Amount"
                );

        dataSet.setValueTextSize(12f);

        dataSet.setColor(
                Color.rgb(11, 143, 77)
        );


        BarData barData =
                new BarData(
                        dataSet
                );

        barData.setBarWidth(0.45f);

        barChart.setData(
                barData
        );


        String[] labels = {
                "Income",
                "Expense"
        };


        barChart.getXAxis()
                .setValueFormatter(
                        new IndexAxisValueFormatter(
                                labels
                        )
                );

        barChart.getXAxis()
                .setLabelCount(
                        2
                );


        barChart.invalidate();

        barChart.animateY(
                700
        );
    }


    // =========================================================
    // PIE CHART
    // =========================================================

    private void updatePieChart(
            Map<String, Double> categoryMap
    ) {

        ArrayList<PieEntry> entries =
                new ArrayList<>();

        for (
                Map.Entry<String, Double> entry :
                categoryMap.entrySet()
        ) {

            if (entry.getValue() > 0) {

                entries.add(
                        new PieEntry(
                                entry.getValue().floatValue(),
                                entry.getKey()
                        )
                );
            }
        }


        if (entries.isEmpty()) {

            pieChart.clear();

            pieChart.setCenterText(
                    "No Expenses"
            );

            pieChart.invalidate();

            return;
        }


        PieDataSet dataSet =
                new PieDataSet(
                        entries,
                        ""
                );

        dataSet.setSliceSpace(
                3f
        );

        dataSet.setValueTextSize(
                11f
        );


        ArrayList<Integer> colors =
                new ArrayList<>();

        colors.add(
                Color.rgb(11, 143, 77)
        );

        colors.add(
                Color.rgb(255, 152, 0)
        );

        colors.add(
                Color.rgb(33, 150, 243)
        );

        colors.add(
                Color.rgb(156, 39, 176)
        );

        colors.add(
                Color.rgb(244, 67, 54)
        );

        colors.add(
                Color.rgb(0, 188, 212)
        );

        colors.add(
                Color.rgb(121, 85, 72)
        );


        dataSet.setColors(
                colors
        );


        PieData pieData =
                new PieData(
                        dataSet
                );


        pieChart.setData(
                pieData
        );

        pieChart.setCenterText(
                "Expenses"
        );

        pieChart.invalidate();

        pieChart.animateY(
                700
        );
    }


    // =========================================================
    // CATEGORY LIST
    // =========================================================

    private void updateCategoryList(
            Map<String, Double> categoryMap,
            double totalExpense
    ) {

        categoryContainer.removeAllViews();


        if (categoryMap.isEmpty() ||
                totalExpense <= 0) {

            tvNoCategory.setVisibility(
                    View.VISIBLE
            );

            return;

        } else {

            tvNoCategory.setVisibility(
                    View.GONE
            );
        }


        List<Map.Entry<String, Double>> list =
                new ArrayList<>(
                        categoryMap.entrySet()
                );


        Collections.sort(
                list,
                (a, b) ->
                        Double.compare(
                                b.getValue(),
                                a.getValue()
                        )
        );


        for (
                Map.Entry<String, Double> entry :
                list
        ) {

            String category =
                    entry.getKey();

            double amount =
                    entry.getValue();

            double percentage =
                    (amount / totalExpense) * 100;


            LinearLayout row =
                    new LinearLayout(
                            this
                    );

            row.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            row.setGravity(
                    Gravity.CENTER_VERTICAL
            );

            row.setPadding(
                    0,
                    12,
                    0,
                    12
            );


            TextView categoryText =
                    new TextView(
                            this
                    );

            categoryText.setText(
                    category
            );

            categoryText.setTextSize(
                    15
            );

            categoryText.setTextColor(
                    Color.rgb(30, 30, 30)
            );

            categoryText.setTypeface(
                    null,
                    Typeface.BOLD
            );


            LinearLayout.LayoutParams categoryParams =
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1
                    );


            row.addView(
                    categoryText,
                    categoryParams
            );


            TextView percentText =
                    new TextView(
                            this
                    );

            percentText.setText(
                    String.format(
                            Locale.US,
                            "%.0f%%",
                            percentage
                    )
            );

            percentText.setTextSize(
                    13
            );

            percentText.setTextColor(
                    Color.GRAY
            );


            TextView amountText =
                    new TextView(
                            this
                    );

            amountText.setText(
                    formatMoney(amount)
            );

            amountText.setTextSize(
                    14
            );

            amountText.setTextColor(
                    Color.rgb(244, 67, 54)
            );

            amountText.setTypeface(
                    null,
                    Typeface.BOLD
            );

            amountText.setPadding(
                    15,
                    0,
                    0,
                    0
            );


            row.addView(
                    percentText
            );

            row.addView(
                    amountText
            );


            categoryContainer.addView(
                    row
            );
        }
    }


    // =========================================================
    // TRANSACTIONS
    // =========================================================

    private void updateTransactions(
            List<QueryDocumentSnapshot> transactions
    ) {

        transactionContainer.removeAllViews();


        if (transactions.isEmpty()) {

            tvNoTransactions.setVisibility(
                    View.VISIBLE
            );

            return;

        } else {

            tvNoTransactions.setVisibility(
                    View.GONE
            );
        }


        // Newest first
        Collections.sort(
                transactions,
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

                        return time2.compareTo(
                                time1
                        );
                    }
                }
        );


        for (
                QueryDocumentSnapshot document :
                transactions
        ) {

            addTransactionRow(
                    document
            );
        }
    }


    // =========================================================
    // TRANSACTION ROW
    // =========================================================

    private void addTransactionRow(
            QueryDocumentSnapshot document
    ) {

        String type =
                document.getString("type");

        String title =
                document.getString("title");

        String category =
                document.getString("category");

        String date =
                document.getString("date");

        double amount =
                getAmount(document);


        boolean isIncome =
                "income".equals(type);


        LinearLayout row =
                new LinearLayout(
                        this
                );

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        row.setPadding(
                12,
                14,
                12,
                14
        );


        // ==============================
        // LEFT
        // ==============================

        TextView icon =
                new TextView(
                        this
                );

        icon.setText(
                isIncome ? "↑" : "↓"
        );

        icon.setTextSize(
                20
        );

        icon.setGravity(
                Gravity.CENTER
        );

        icon.setTextColor(
                isIncome
                        ? Color.rgb(11, 143, 77)
                        : Color.rgb(244, 67, 54)
        );


        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(
                        45,
                        45
                );

        row.addView(
                icon,
                iconParams
        );


        // ==============================
        // MIDDLE
        // ==============================

        LinearLayout middle =
                new LinearLayout(
                        this
                );

        middle.setOrientation(
                LinearLayout.VERTICAL
        );

        middle.setPadding(
                12,
                0,
                8,
                0
        );


        TextView titleText =
                new TextView(
                        this
                );

        if (title == null ||
                title.isEmpty()) {

            title = "Transaction";
        }

        titleText.setText(
                title
        );

        titleText.setTextSize(
                15
        );

        titleText.setTextColor(
                Color.rgb(30, 30, 30)
        );

        titleText.setTypeface(
                null,
                Typeface.BOLD
        );


        TextView detailsText =
                new TextView(
                        this
                );

        detailsText.setText(
                (category != null
                        ? category
                        : "")
                        + " • "
                        + (date != null
                        ? date
                        : "")
        );

        detailsText.setTextSize(
                12
        );

        detailsText.setTextColor(
                Color.GRAY
        );

        detailsText.setPadding(
                0,
                4,
                0,
                0
        );


        middle.addView(
                titleText
        );

        middle.addView(
                detailsText
        );


        LinearLayout.LayoutParams middleParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        row.addView(
                middle,
                middleParams
        );


        // ==============================
        // AMOUNT
        // ==============================

        TextView amountText =
                new TextView(
                        this
                );

        amountText.setText(
                (isIncome ? "+₹" : "-₹")
                        + formatNumber(amount)
        );

        amountText.setTextSize(
                15
        );

        amountText.setTypeface(
                null,
                Typeface.BOLD
        );

        amountText.setTextColor(
                isIncome
                        ? Color.rgb(11, 143, 77)
                        : Color.rgb(244, 67, 54)
        );


        row.addView(
                amountText
        );


        transactionContainer.addView(
                row
        );


        // Divider
        View divider =
                new View(
                        this
                );

        divider.setBackgroundColor(
                Color.rgb(
                        235,
                        235,
                        235
                )
        );


        transactionContainer.addView(
                divider,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                )
        );
    }


    // =========================================================
    // RESET
    // =========================================================

    private void resetReports() {

        tvTotalIncome.setText(
                "₹0"
        );

        tvTotalExpense.setText(
                "₹0"
        );

        tvSavings.setText(
                "₹0"
        );


        if (barChart != null) {

            barChart.clear();

            barChart.invalidate();
        }


        if (pieChart != null) {

            pieChart.clear();

            pieChart.setCenterText(
                    "No Expenses"
            );

            pieChart.invalidate();
        }


        if (categoryContainer != null) {

            categoryContainer.removeAllViews();
        }


        if (transactionContainer != null) {

            transactionContainer.removeAllViews();
        }


        if (tvNoCategory != null) {

            tvNoCategory.setVisibility(
                    View.VISIBLE
            );
        }


        if (tvNoTransactions != null) {

            tvNoTransactions.setVisibility(
                    View.VISIBLE
            );
        }
    }


    // =========================================================
    // GET AMOUNT
    // =========================================================

    private double getAmount(
            QueryDocumentSnapshot document
    ) {

        Object amountObject =
                document.get("amount");

        if (amountObject == null) {
            return 0;
        }

        try {

            return Double.parseDouble(
                    amountObject.toString()
            );

        } catch (Exception e) {

            return 0;
        }
    }


    // =========================================================
    // MONEY FORMAT
    // =========================================================

    private String formatMoney(
            double amount
    ) {

        return "₹" +
                formatNumber(amount);
    }


    private String formatNumber(
            double amount
    ) {

        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        new Locale(
                                "en",
                                "IN"
                        )
                );

        formatter.setMaximumFractionDigits(
                0
        );

        return formatter.format(
                amount
        );
    }


    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private void setupBottomNavigation() {

        bottomNavigation.setSelectedItemId(
                R.id.nav_reports
        );


        bottomNavigation.setOnItemSelectedListener(
                item -> {

                    int id =
                            item.getItemId();


                    if (id == R.id.nav_home) {

                        startActivity(
                                new Intent(
                                        ReportsActivity.this,
                                        DashboardActivity.class
                                )
                        );

                        finish();

                        return true;


                    } else if (
                            id == R.id.nav_reports
                    ) {

                        return true;


                    } else if (
                            id == R.id.nav_add
                    ) {

                        startActivity(
                                new Intent(
                                        ReportsActivity.this,
                                        AddTransactionActivity.class
                                )
                        );

                        finish();

                        return true;


                    } else if (
                            id == R.id.nav_ai
                    ) {

                        Toast.makeText(
                                this,
                                "AI Assistant coming soon",
                                Toast.LENGTH_SHORT
                        ).show();

                        return true;


                    } else if (
                            id == R.id.nav_profile
                    ) {

                        Toast.makeText(
                                this,
                                "Profile coming soon",
                                Toast.LENGTH_SHORT
                        ).show();

                        return true;
                    }

                    return false;
                }
        );
    }


    // =========================================================
    // REFRESH
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (mAuth != null) {

            loadTransactions();
        }
    }
}