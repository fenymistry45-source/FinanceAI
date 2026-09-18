package com.example.financeai;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AIAssistantActivity extends AppCompatActivity {

    // =========================================================
    // UI
    // =========================================================

    private LinearLayout chatContainer;
    private ScrollView chatScrollView;

    private EditText edtMessage;
    private ImageButton btnSend;

    // =========================================================
    // FIREBASE
    // =========================================================

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // =========================================================
    // FINANCIAL DATA
    // =========================================================

    private double totalIncome = 0;
    private double totalExpense = 0;

    private double currentMonthIncome = 0;
    private double currentMonthExpense = 0;

    private final Map<String, Double> categoryExpenses =
            new HashMap<>();

    // =========================================================
    // TRANSACTIONS
    // =========================================================

    private final List<TransactionData> transactions =
            new ArrayList<>();

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_aiassistant);

        // Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Views
        chatContainer =
                findViewById(R.id.chatContainer);

        chatScrollView =
                findViewById(R.id.chatScrollView);

        edtMessage =
                findViewById(R.id.edtMessage);

        btnSend =
                findViewById(R.id.btnSend);

        // Load financial data
        loadFinancialData();

        // Load previous 24 hour chat
        loadChatHistory();

        // =====================================================
        // SEND BUTTON
        // =====================================================

        btnSend.setOnClickListener(v -> {

            String message =
                    edtMessage.getText()
                            .toString()
                            .trim();

            if (message.isEmpty()) {
                return;
            }

            sendUserMessage(message);
        });

        // Enter key support
        edtMessage.setOnEditorActionListener(
                (v, actionId, event) -> {

                    String message =
                            edtMessage.getText()
                                    .toString()
                                    .trim();

                    if (!message.isEmpty()) {

                        sendUserMessage(message);

                        return true;
                    }

                    return false;
                }
        );
    }

    // =========================================================
    // LOAD FINANCIAL DATA
    // =========================================================

    private void loadFinancialData() {

        if (auth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Please login first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String userId =
                auth.getCurrentUser().getUid();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(
                        this::processTransactions
                )
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Unable to load transaction data",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // PROCESS TRANSACTIONS
    // =========================================================

    private void processTransactions(
            QuerySnapshot snapshot) {

        totalIncome = 0;
        totalExpense = 0;

        currentMonthIncome = 0;
        currentMonthExpense = 0;

        categoryExpenses.clear();
        transactions.clear();

        Calendar now =
                Calendar.getInstance();

        int currentMonth =
                now.get(Calendar.MONTH);

        int currentYear =
                now.get(Calendar.YEAR);

        for (QueryDocumentSnapshot document :
                snapshot) {

            String type =
                    document.getString("type");

            Double amount =
                    getAmount(document);

            String category =
                    document.getString("category");

            String title =
                    document.getString("title");

            String notes =
                    document.getString("notes");

            String date =
                    document.getString("date");

            Long createdAt =
                    getCreatedAt(document);

            if (type == null ||
                    amount == null) {

                continue;
            }

            if (category == null ||
                    category.trim().isEmpty()) {

                category = "Other";
            }

            if (title == null ||
                    title.trim().isEmpty()) {

                title = "Transaction";
            }

            if (notes == null) {
                notes = "";
            }

            // Save local transaction
            TransactionData transaction =
                    new TransactionData();

            transaction.type = type;
            transaction.amount = amount;
            transaction.category = category;
            transaction.title = title;
            transaction.notes = notes;
            transaction.date = date;
            transaction.createdAt =
                    createdAt != null
                            ? createdAt
                            : 0;

            transactions.add(transaction);

            // =================================================
            // INCOME
            // =================================================

            if (type.equalsIgnoreCase("income")) {

                totalIncome += amount;
            }

            // =================================================
            // EXPENSE
            // =================================================

            if (type.equalsIgnoreCase("expense")) {

                totalExpense += amount;

                double oldAmount =
                        categoryExpenses.containsKey(
                                category
                        )
                                ? categoryExpenses.get(
                                category
                        )
                                : 0;

                categoryExpenses.put(
                        category,
                        oldAmount + amount
                );
            }

            // =================================================
            // CURRENT MONTH
            // =================================================

            if (date != null) {

                Calendar transactionDate =
                        parseTransactionDate(date);

                if (transactionDate != null) {

                    int transactionMonth =
                            transactionDate.get(
                                    Calendar.MONTH
                            );

                    int transactionYear =
                            transactionDate.get(
                                    Calendar.YEAR
                            );

                    if (transactionMonth ==
                            currentMonth &&
                            transactionYear ==
                                    currentYear) {

                        if (type.equalsIgnoreCase(
                                "income")) {

                            currentMonthIncome +=
                                    amount;

                        } else if (
                                type.equalsIgnoreCase(
                                        "expense")) {

                            currentMonthExpense +=
                                    amount;
                        }
                    }
                }
            }
        }
    }

    // =========================================================
    // GET AMOUNT
    // =========================================================

    private Double getAmount(
            QueryDocumentSnapshot document) {

        Object value =
                document.get("amount");

        if (value == null) {
            return null;
        }

        if (value instanceof Number) {

            return ((Number) value)
                    .doubleValue();
        }

        try {

            return Double.parseDouble(
                    value.toString()
            );

        } catch (Exception e) {

            return null;
        }
    }

    // =========================================================
    // GET CREATED AT
    // =========================================================

    private Long getCreatedAt(
            QueryDocumentSnapshot document) {

        Object value =
                document.get("createdAt");

        if (value instanceof Number) {

            return ((Number) value)
                    .longValue();
        }

        return 0L;
    }

    // =========================================================
    // PARSE DATE
    // =========================================================

    private Calendar parseTransactionDate(
            String dateString) {

        try {

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "d/M/yyyy",
                            Locale.getDefault()
                    );

            format.setLenient(false);

            Date date =
                    format.parse(dateString);

            Calendar calendar =
                    Calendar.getInstance();

            calendar.setTime(date);

            return calendar;

        } catch (Exception e) {

            return null;
        }
    }

    // =========================================================
    // SEND USER MESSAGE
    // =========================================================

    private void sendUserMessage(
            String message) {

        // Show user message
        addUserMessage(message);

        // Save user message
        saveChatMessage(
                message,
                "user"
        );

        // Clear text AFTER reading message
        edtMessage.setText("");

        // Keep keyboard open
        edtMessage.requestFocus();

        // Generate local AI answer
        String answer =
                generateAIResponse(message);

        // Show AI response
        addAIMessage(answer);

        // Save AI response
        saveChatMessage(
                answer,
                "ai"
        );

        scrollToBottom();
    }

    // =========================================================
    // GENERATE AI RESPONSE
    // =========================================================

    private String generateAIResponse(
            String message) {

        String question =
                message.toLowerCase(
                        Locale.getDefault()
                ).trim();

        // =====================================================
        // GREETING
        // =====================================================

        if (containsAny(
                question,
                "hi",
                "hello",
                "hey",
                "hii",
                "helo"
        )) {

            return "👋 Hello! I'm your Finance AI Assistant.\n\n"
                    + "You can ask me things like:\n"
                    + "• What is my total income?\n"
                    + "• How much did I spend?\n"
                    + "• What is my savings?\n"
                    + "• How much did I spend on Food?\n"
                    + "• What is my monthly expense?";
        }

        // =====================================================
        // TOTAL INCOME
        // =====================================================

        if (containsAny(
                question,
                "total income",
                "income total",
                "my income",
                "income ketlu",
                "income ketlo",
                "income"
        )) {

            return "💰 Your total income is "
                    + formatCurrency(totalIncome)
                    + ".";
        }

        // =====================================================
        // TOTAL EXPENSE
        // =====================================================

        if (containsAny(
                question,
                "total expense",
                "total expenses",
                "my expense",
                "my expenses",
                "expense ketlo",
                "expense ketlu",
                "expenses",
                "kharcho",
                "kharch"
        )) {

            return "💸 Your total expense is "
                    + formatCurrency(totalExpense)
                    + ".";
        }

        // =====================================================
        // CURRENT MONTH INCOME
        // =====================================================

        if (containsAny(
                question,
                "this month income",
                "current month income",
                "monthly income",
                "aa month income",
                "aa mahina ni income"
        )) {

            return "📅 Your income this month is "
                    + formatCurrency(
                    currentMonthIncome
            )
                    + ".";
        }

        // =====================================================
        // CURRENT MONTH EXPENSE
        // =====================================================

        if (containsAny(
                question,
                "this month expense",
                "current month expense",
                "monthly expense",
                "aa month expense",
                "aa mahina no kharch"
        )) {

            return "📅 Your expense this month is "
                    + formatCurrency(
                    currentMonthExpense
            )
                    + ".";
        }

        // =====================================================
        // SAVINGS
        // =====================================================

        if (containsAny(
                question,
                "total saving",
                "total savings",
                "my saving",
                "my savings",
                "savings ketli",
                "saving ketli",
                "saving ketlu"
        )) {

            double savings =
                    totalIncome - totalExpense;

            if (savings >= 0) {

                return "💰 Your total savings are "
                        + formatCurrency(savings)
                        + ".\n\n"
                        + "Total income: "
                        + formatCurrency(totalIncome)
                        + "\n"
                        + "Total expense: "
                        + formatCurrency(totalExpense);

            } else {

                return "⚠️ Your expenses are "
                        + formatCurrency(
                        Math.abs(savings)
                )
                        + " higher than your income.";
            }
        }

        // =====================================================
        // CURRENT MONTH SAVINGS
        // =====================================================

        if (containsAny(
                question,
                "this month saving",
                "this month savings",
                "monthly saving",
                "monthly savings",
                "aa month saving",
                "aa mahina ni saving"
        )) {

            double savings =
                    currentMonthIncome
                            - currentMonthExpense;

            return "📊 This month's financial summary:\n\n"
                    + "Income: "
                    + formatCurrency(
                    currentMonthIncome
            )
                    + "\n"
                    + "Expense: "
                    + formatCurrency(
                    currentMonthExpense
            )
                    + "\n"
                    + "Savings: "
                    + formatCurrency(savings);
        }

        // =====================================================
        // BALANCE
        // =====================================================

        if (containsAny(
                question,
                "balance",
                "remaining money",
                "money left",
                "ketla paisa baki",
                "paisa baki"
        )) {

            double balance =
                    totalIncome - totalExpense;

            return "💵 Your current balance based on "
                    + "recorded transactions is "
                    + formatCurrency(balance)
                    + ".";
        }

        // =====================================================
        // CATEGORY EXPENSE
        // =====================================================

        String matchedCategory =
                findCategoryInQuestion(question);

        if (matchedCategory != null) {

            double amount =
                    getCategoryAmount(
                            matchedCategory
                    );

            return "📂 You have spent "
                    + formatCurrency(amount)
                    + " on "
                    + matchedCategory
                    + ".";
        }

        // =====================================================
        // HIGHEST EXPENSE CATEGORY
        // =====================================================

        if (containsAny(
                question,
                "highest expense",
                "most expense",
                "maximum expense",
                "highest spending",
                "most spending",
                "which category"
        )) {

            if (categoryExpenses.isEmpty()) {

                return "📊 I don't have any expense "
                        + "category data yet.";
            }

            String highestCategory = "";
            double highestAmount = 0;

            for (Map.Entry<String, Double> entry :
                    categoryExpenses.entrySet()) {

                if (entry.getValue() >
                        highestAmount) {

                    highestAmount =
                            entry.getValue();

                    highestCategory =
                            entry.getKey();
                }
            }

            return "📊 Your highest spending category "
                    + "is "
                    + highestCategory
                    + " with "
                    + formatCurrency(
                    highestAmount
            )
                    + " spent.";
        }

        // =====================================================
        // RECENT TRANSACTIONS
        // =====================================================

        if (containsAny(
                question,
                "recent transaction",
                "recent transactions",
                "latest transaction",
                "latest transactions",
                "recent expense",
                "recent income"
        )) {

            return getRecentTransactions();
        }

        // =====================================================
        // EXPENSE PERCENTAGE
        // =====================================================

        if (containsAny(
                question,
                "expense percentage",
                "spending percentage",
                "expense percent"
        )) {

            if (totalIncome <= 0) {

                return "📊 I can't calculate the expense "
                        + "percentage because your total income "
                        + "is ₹0.";
            }

            double percentage =
                    (totalExpense / totalIncome)
                            * 100;

            return String.format(
                    Locale.getDefault(),
                    "📊 Your expenses are %.1f%% of your total income.",
                    percentage
            );
        }

        // =====================================================
        // BUDGET ADVICE
        // =====================================================

        if (containsAny(
                question,
                "budget",
                "save money",
                "saving advice",
                "how can i save",
                "how to save",
                "advice",
                "suggestion"
        )) {

            if (totalIncome <= 0) {

                return "💡 I don't have any recorded income yet.\n\n"
                        + "Add your income transactions first, "
                        + "then I can give you more useful "
                        + "budget advice.";
            }

            double savings =
                    totalIncome - totalExpense;

            double expensePercentage =
                    (totalExpense / totalIncome)
                            * 100;

            if (expensePercentage > 80) {

                return "💡 Your expenses are "
                        + String.format(
                        Locale.getDefault(),
                        "%.1f",
                        expensePercentage
                )
                        + "% of your income.\n\n"
                        + "Try reducing unnecessary spending "
                        + "and keep some money aside as savings.";

            } else if (expensePercentage > 50) {

                return "💡 Your expenses are "
                        + String.format(
                        Locale.getDefault(),
                        "%.1f",
                        expensePercentage
                )
                        + "% of your income.\n\n"
                        + "Your spending is manageable, but "
                        + "you can still increase your savings "
                        + "by reducing non-essential expenses.";

            } else {

                return "🎉 Good job!\n\n"
                        + "Your expenses are only "
                        + String.format(
                        Locale.getDefault(),
                        "%.1f",
                        expensePercentage
                )
                        + "% of your income.\n\n"
                        + "You are maintaining a healthy "
                        + "saving level.";
            }
        }

        // =====================================================
        // DATA STATUS
        // =====================================================

        if (containsAny(
                question,
                "do you have my data",
                "my data",
                "data available",
                "transaction data"
        )) {

            return "📊 Yes. I use the transactions "
                    + "stored in your Finance AI account.\n\n"
                    + "Total income: "
                    + formatCurrency(totalIncome)
                    + "\n"
                    + "Total expense: "
                    + formatCurrency(totalExpense);
        }

        // =====================================================
        // DEFAULT RESPONSE
        // =====================================================

        return "🤖 I can help you with your Finance AI data.\n\n"
                + "Try asking:\n"
                + "• What is my total income?\n"
                + "• What is my total expense?\n"
                + "• How much did I save?\n"
                + "• How much did I spend on Food?\n"
                + "• What is my highest expense?\n"
                + "• Show recent transactions\n"
                + "• Give me budget advice.";
    }

    // =========================================================
    // FIND CATEGORY
    // =========================================================

    private String findCategoryInQuestion(
            String question) {

        for (String category :
                categoryExpenses.keySet()) {

            if (question.contains(
                    category.toLowerCase(
                            Locale.getDefault()
                    )
            )) {

                return category;
            }
        }

        // Common categories
        String[] commonCategories = {
                "food",
                "travel",
                "shopping",
                "bills",
                "petrol",
                "grocery",
                "other"
        };

        for (String category :
                commonCategories) {

            if (question.contains(category)) {

                for (String storedCategory :
                        categoryExpenses.keySet()) {

                    if (storedCategory
                            .equalsIgnoreCase(
                                    category
                            )) {

                        return storedCategory;
                    }
                }

                return capitalize(category);
            }
        }

        return null;
    }

    // =========================================================
    // CATEGORY AMOUNT
    // =========================================================

    private double getCategoryAmount(
            String category) {

        if (categoryExpenses.containsKey(category)) {

            return categoryExpenses.get(category);
        }

        for (Map.Entry<String, Double> entry :
                categoryExpenses.entrySet()) {

            if (entry.getKey()
                    .equalsIgnoreCase(category)) {

                return entry.getValue();
            }
        }

        return 0;
    }

    // =========================================================
    // RECENT TRANSACTIONS
    // =========================================================

    private String getRecentTransactions() {

        if (transactions.isEmpty()) {

            return "📭 You don't have any transactions yet.";
        }

        List<TransactionData> sorted =
                new ArrayList<>(transactions);

        sorted.sort(
                (a, b) ->
                        Long.compare(
                                b.createdAt,
                                a.createdAt
                        )
        );

        StringBuilder result =
                new StringBuilder();

        result.append(
                "📋 Your recent transactions:\n\n"
        );

        int count =
                Math.min(
                        5,
                        sorted.size()
                );

        for (int i = 0; i < count; i++) {

            TransactionData transaction =
                    sorted.get(i);

            String symbol =
                    transaction.type.equalsIgnoreCase(
                            "income"
                    )
                            ? "🟢"
                            : "🔴";

            result.append(symbol)
                    .append(" ")
                    .append(transaction.title)
                    .append(" - ")
                    .append(
                            formatCurrency(
                                    transaction.amount
                            )
                    )
                    .append("\n");

            result.append("Category: ")
                    .append(transaction.category)
                    .append("\n");

            if (transaction.date != null) {

                result.append("Date: ")
                        .append(transaction.date)
                        .append("\n");
            }

            result.append("\n");
        }

        return result.toString().trim();
    }

    // =========================================================
    // CHECK KEYWORDS
    // =========================================================

    private boolean containsAny(
            String text,
            String... words) {

        for (String word : words) {

            if (text.contains(word)) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // CAPITALIZE
    // =========================================================

    private String capitalize(
            String text) {

        if (text == null ||
                text.isEmpty()) {

            return text;
        }

        return text.substring(0, 1)
                .toUpperCase(
                        Locale.getDefault()
                )
                + text.substring(1);
    }

    // =========================================================
    // SAVE CHAT MESSAGE
    // =========================================================

    private void saveChatMessage(
            String message,
            String sender) {

        if (auth.getCurrentUser() == null) {
            return;
        }

        String userId =
                auth.getCurrentUser()
                        .getUid();

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "message",
                message
        );

        data.put(
                "sender",
                sender
        );

        data.put(
                "timestamp",
                System.currentTimeMillis()
        );

        db.collection("users")
                .document(userId)
                .collection("ai_chats")
                .add(data);
    }

    // =========================================================
    // LOAD LAST 24 HOURS CHAT
    // =========================================================

    private void loadChatHistory() {

        if (auth.getCurrentUser() == null) {
            return;
        }

        String userId =
                auth.getCurrentUser()
                        .getUid();

        long twentyFourHoursAgo =
                System.currentTimeMillis()
                        - (24L * 60L * 60L * 1000L);

        db.collection("users")
                .document(userId)
                .collection("ai_chats")
                .whereGreaterThan(
                        "timestamp",
                        twentyFourHoursAgo
                )
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            List<ChatData> chats =
                                    new ArrayList<>();

                            for (QueryDocumentSnapshot document :
                                    snapshot) {

                                String message =
                                        document.getString(
                                                "message"
                                        );

                                String sender =
                                        document.getString(
                                                "sender"
                                        );

                                Long timestamp =
                                        getChatTimestamp(
                                                document
                                        );

                                if (message == null ||
                                        sender == null) {

                                    continue;
                                }

                                ChatData chat =
                                        new ChatData();

                                chat.message =
                                        message;

                                chat.sender =
                                        sender;

                                chat.timestamp =
                                        timestamp;

                                chats.add(chat);
                            }

                            // Oldest → newest
                            chats.sort(
                                    (a, b) ->
                                            Long.compare(
                                                    a.timestamp,
                                                    b.timestamp
                                            )
                            );

                            for (ChatData chat :
                                    chats) {

                                if (chat.sender.equals(
                                        "user"
                                )) {

                                    addUserMessage(
                                            chat.message
                                    );

                                } else {

                                    addAIMessage(
                                            chat.message
                                    );
                                }
                            }

                            scrollToBottom();
                        }
                );
    }

    // =========================================================
    // CHAT TIMESTAMP
    // =========================================================

    private Long getChatTimestamp(
            QueryDocumentSnapshot document) {

        Object value =
                document.get("timestamp");

        if (value instanceof Number) {

            return ((Number) value)
                    .longValue();
        }

        return 0L;
    }

    // =========================================================
    // USER MESSAGE UI
    // =========================================================

    private void addUserMessage(
            String message) {

        LinearLayout row =
                new LinearLayout(this);

        // IMPORTANT:
        // setWidth() NO use.
        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        row.setLayoutParams(rowParams);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.END
        );

        TextView text =
                new TextView(this);

        text.setText(message);

        text.setTextColor(
                Color.WHITE
        );

        text.setTextSize(15);

        text.setPadding(
                16,
                12,
                16,
                12
        );

        text.setBackgroundResource(
                R.drawable.bg_user_message
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                50,
                6,
                0,
                6
        );

        text.setLayoutParams(params);

        row.addView(text);

        chatContainer.addView(row);

        scrollToBottom();
    }

    // =========================================================
    // AI MESSAGE UI
    // =========================================================

    private void addAIMessage(
            String message) {

        LinearLayout row =
                new LinearLayout(this);

        // IMPORTANT:
        // setWidth() NO use.
        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        row.setLayoutParams(rowParams);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.START
        );

        TextView icon =
                new TextView(this);

        icon.setText("🤖");

        icon.setTextSize(20);

        icon.setGravity(
                Gravity.CENTER
        );

        icon.setBackgroundResource(
                R.drawable.bg_ai_icon
        );

        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(
                        42,
                        42
                );

        iconParams.setMargins(
                0,
                6,
                8,
                6
        );

        icon.setLayoutParams(iconParams);

        TextView text =
                new TextView(this);

        text.setText(message);

        text.setTextColor(
                Color.DKGRAY
        );

        text.setTextSize(15);

        text.setPadding(
                16,
                12,
                16,
                12
        );

        text.setBackgroundResource(
                R.drawable.bg_ai_message
        );

        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        textParams.setMargins(
                0,
                6,
                50,
                6
        );

        text.setLayoutParams(textParams);

        row.addView(icon);
        row.addView(text);

        chatContainer.addView(row);

        scrollToBottom();
    }

    // =========================================================
    // SCROLL
    // =========================================================

    private void scrollToBottom() {

        if (chatScrollView == null) {
            return;
        }

        chatScrollView.post(() ->
                chatScrollView.fullScroll(
                        View.FOCUS_DOWN
                )
        );
    }

    // =========================================================
    // CURRENCY
    // =========================================================

    private String formatCurrency(
            double amount) {

        NumberFormat formatter =
                NumberFormat.getCurrencyInstance(
                        new Locale(
                                "en",
                                "IN"
                        )
                );

        return formatter.format(amount);
    }

    // =========================================================
    // TRANSACTION DATA CLASS
    // =========================================================

    private static class TransactionData {

        String type;
        double amount;
        String category;
        String title;
        String notes;
        String date;
        long createdAt;
    }

    // =========================================================
    // CHAT DATA CLASS
    // =========================================================

    private static class ChatData {

        String message;
        String sender;
        long timestamp;
    }
}