package com.archtech.collegealert;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Notification;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Typeface;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String CHANNEL_ID = "college_events";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout eventList;
    private TextView status;
    private SharedPreferences prefs;
    private boolean firstLoad = true;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("college_alert", MODE_PRIVATE);
        createChannel();
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 77);
        }
        buildScreen();
        loadEvents();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(246, 248, 252));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(20));
        scroll.addView(root);

        TextView heading = new TextView(this);
        heading.setText("College Alert");
        heading.setTextSize(28);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(Color.rgb(24, 48, 92));
        root.addView(heading);

        TextView subtitle = new TextView(this);
        subtitle.setText("Stay updated with campus events");
        subtitle.setTextSize(15);
        subtitle.setTextColor(Color.DKGRAY);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.bottomMargin = dp(18);
        root.addView(subtitle, subParams);

        Button refresh = new Button(this);
        refresh.setText("Refresh events");
        root.addView(refresh);
        refresh.setOnClickListener(v -> loadEvents());

        status = new TextView(this);
        status.setText("Loading events...");
        status.setTextColor(Color.DKGRAY);
        status.setPadding(0, dp(12), 0, dp(12));
        root.addView(status);

        eventList = new LinearLayout(this);
        eventList.setOrientation(LinearLayout.VERTICAL);
        root.addView(eventList);
        setContentView(scroll);
    }

    private void loadEvents() {
        status.setText("Connecting to college server...");
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BuildConfig.API_BASE_URL + "api/events");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                int code = connection.getResponseCode();
                if (code != 200) throw new Exception("Server returned " + code);
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
                reader.close();
                JSONObject response = new JSONObject(body.toString());
                JSONArray events = response.getJSONArray("events");
                runOnUiThread(() -> renderEvents(events));
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Could not load events. Check that the backend is running and API address is correct. (" + e.getMessage() + ")"));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void renderEvents(JSONArray events) {
        eventList.removeAllViews();
        status.setText(events.length() + " campus event(s)");
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event == null) continue;
            int id = event.optInt("id");
            String title = event.optString("title", "Campus event");
            String description = event.optString("description", "");
            String date = event.optString("event_date", "");
            String time = event.optString("event_time", "");
            String venue = event.optString("venue", "");

            if (!firstLoad && !prefs.getBoolean("seen_" + id, false)) {
                showEventNotification(title, date);
            }
            prefs.edit().putBoolean("seen_" + id, true).apply();

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            card.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.bottomMargin = dp(12);
            card.setElevation(dp(2));

            TextView name = new TextView(this);
            name.setText(title);
            name.setTextSize(19);
            name.setTypeface(null, Typeface.BOLD);
            name.setTextColor(Color.rgb(25, 58, 110));
            card.addView(name);

            TextView details = new TextView(this);
            details.setText("📅  " + date + "   •   " + time + "\n📍  " + venue + "\n\n" + description);
            details.setTextSize(14);
            details.setTextColor(Color.rgb(65, 72, 85));
            details.setPadding(0, dp(8), 0, 0);
            card.addView(details);
            eventList.addView(card, cardParams);
        }
        if (events.length() == 0) status.setText("No events yet. Please check again later.");
        firstLoad = false;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "College events", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Alerts about newly added college events");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void showEventNotification(String title, String date) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("New college event: " + title)
                .setContentText("Scheduled for " + date)
                .setAutoCancel(true);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify((int) (System.currentTimeMillis() & 0xfffffff), builder.build());
    }

    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
