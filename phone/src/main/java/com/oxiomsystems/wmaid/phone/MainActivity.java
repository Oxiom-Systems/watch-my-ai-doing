package com.oxiomsystems.wmaid.phone;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import com.oxiomsystems.wmaid.core.AlertItem;
import com.oxiomsystems.wmaid.core.WmaidRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private WmaidRepository repository;
    private PhoneView phoneView;
    private boolean notified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new WmaidRepository(this);
        phoneView = new PhoneView();
        setContentView(phoneView);
        createNotificationChannel();
    }

    @Override
    protected void onDestroy() {
        repository.close();
        super.onDestroy();
    }

    private void connect() {
        phoneView.state = Screen.CONNECTING;
        phoneView.error = "";
        phoneView.invalidate();
        repository.connect(() -> {
            repository.observe(alerts -> {
                phoneView.alerts = alerts;
                AlertItem waiting = firstWaiting(alerts);
                phoneView.state = waiting != null && waiting.handoffRequested
                        ? Screen.PROVIDER
                        : Screen.DASHBOARD;
                phoneView.invalidate();
                if (waiting != null && !notified) {
                    notified = true;
                    showNotification(waiting);
                }
            }, phoneView::showError);
        }, phoneView::showError);
    }

    private static AlertItem firstWaiting(List<AlertItem> alerts) {
        for (AlertItem item : alerts) {
            if (item.isWaiting()) {
                return item;
            }
        }
        return null;
    }

    private static AlertItem primaryAlert(List<AlertItem> alerts) {
        AlertItem waiting = firstWaiting(alerts);
        return waiting != null ? waiting : (alerts.isEmpty() ? null : alerts.get(0));
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                "agent_waiting",
                "AI sessions waiting",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Prompts, permissions, overloads, and errors from AI sessions");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private void showNotification(AlertItem alert) {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Intent open = new Intent(this, MainActivity.class)
                .putExtra("alert_id", alert.id)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                10,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Notification.Builder notification = new Notification.Builder(this, "agent_waiting")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(alert.title)
                .setContentText(alert.question)
                .setStyle(new Notification.BigTextStyle().bigText(alert.question))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE);
        getSystemService(NotificationManager.class).notify(101, notification.build());
    }

    private enum Screen {
        SIGN_IN,
        CONNECTING,
        DASHBOARD,
        DETAIL,
        PROVIDER
    }

    private final class PhoneView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF primaryButton = new RectF();
        private final RectF heroCard = new RectF();
        private final RectF secondaryButton = new RectF();
        private final int purple = Color.rgb(100, 66, 214);
        private final int ink = Color.rgb(30, 25, 38);
        private final int muted = Color.rgb(103, 95, 116);
        private final int surface = Color.WHITE;
        private final int canvasColor = Color.rgb(247, 245, 250);
        private final int amber = Color.rgb(232, 145, 39);
        private Screen state = Screen.SIGN_IN;
        private List<AlertItem> alerts = new ArrayList<>();
        private String error = "";
        private float density;

        PhoneView() {
            super(MainActivity.this);
            density = getResources().getDisplayMetrics().density;
            setBackgroundColor(canvasColor);
            setContentDescription("WMAID phone showcase");
        }

        @Override
        protected void onDraw(Canvas raw) {
            super.onDraw(raw);
            density = getResources().getDisplayMetrics().density;
            raw.save();
            raw.scale(density, density);
            float width = getWidth() / density;
            float height = getHeight() / density;
            if (state == Screen.SIGN_IN || state == Screen.CONNECTING) {
                drawSignIn(raw, width, height);
            } else if (state == Screen.DASHBOARD) {
                drawDashboard(raw, width, height);
            } else if (state == Screen.DETAIL) {
                drawDetail(raw, width, height);
            } else {
                drawProvider(raw, width, height);
            }
            raw.restore();
        }

        private void drawSignIn(Canvas canvas, float width, float height) {
            brand(canvas, 24, 34);
            pill(canvas, 24, 78, 151, 25, "EMULATOR SHOWCASE", purple, 0x146442D6);
            text(canvas, "Your AI can", 24, 150, 38, ink, true);
            text(canvas, "tap your wrist.", 24, 192, 38, purple, true);
            paragraph(canvas,
                    "Know when Claude Code or Codex needs a decision — even when you walk away from your Mac.",
                    24, 229, width - 48, 16, muted, 23);

            card(canvas, 24, 322, width - 48, 150, 24, surface);
            circle(canvas, 56, 358, 16, 0xFFEDE8FF);
            circle(canvas, 56, 358, 6, purple);
            text(canvas, "Local Firebase demo", 84, 357, 17, ink, true);
            text(canvas, "No cloud credentials required", 84, 381, 13, muted, false);
            connector(canvas, 56, 395, 56, 425);
            circle(canvas, 56, 438, 16, 0xFFFFEED9);
            text(canvas, "Phone + standalone Wear OS", 84, 437, 16, ink, true);
            text(canvas, "Both connect independently", 84, 460, 13, muted, false);

            float buttonY = Math.min(height - 92, 555);
            primaryButton.set(24, buttonY, width - 24, buttonY + 56);
            button(canvas, primaryButton, state == Screen.CONNECTING
                    ? "Connecting securely…" : "Continue as Hannes", purple, Color.WHITE);
            text(canvas, "Fake sign-in • Local emulators • Reset anytime",
                    width / 2, buttonY + 82, 12, muted, false, Paint.Align.CENTER);
            if (!error.isEmpty()) {
                text(canvas, error, 24, buttonY - 18, 12, Color.rgb(184, 55, 68), false);
            }
        }

        private void drawDashboard(Canvas canvas, float width, float height) {
            brand(canvas, 24, 34);
            circle(canvas, width - 42, 34, 16, 0xFFEDE8FF);
            text(canvas, "HZ", width - 42, 38, 10, purple, true, Paint.Align.CENTER);
            text(canvas, "Good afternoon, Hannes", 24, 92, 27, ink, true);
            text(canvas, "3 AI sessions monitored", 24, 118, 14, muted, false);
            pill(canvas, width - 104, 100, 80, 25, "●  LIVE", Color.rgb(36, 131, 85), 0x14248355);

            AlertItem hero = primaryAlert(alerts);
            boolean answered = hero != null && hero.isAnswered();
            float heroHeight = 238;
            heroCard.set(18, 150, width - 18, 150 + heroHeight);
            card(canvas, heroCard.left, heroCard.top, heroCard.width(), heroCard.height(), 26, 0xFF1D1925);
            pill(canvas, 38, 171, 105, 25,
                    hero == null ? "CLAUDE" : hero.provider.toUpperCase(Locale.ROOT),
                    0xFFD4C8FF, 0x246442D6);
            pill(canvas, width - 135, 171, 97, 25,
                    answered ? "✓  ANSWERED" : "●  WAITING",
                    answered ? 0xFF8FE2B9 : 0xFFFFC56B,
                    answered ? 0x24248355 : 0x24E89127);
            text(canvas, hero == null ? "Claude needs a decision" : hero.title,
                    38, 231, 23, Color.WHITE, true);
            paragraph(canvas,
                    answered
                            ? "“" + hero.reply + "” sent from " + hero.answeredBy + "."
                            : hero == null
                                    ? "Approve the Firebase relay architecture and continue?"
                                    : hero.question,
                    38, 261, width - 76, 15, 0xFFD3CDD9, 21);
            line(canvas, 38, 322, width - 38, 322, 0xFF3B3544, 1);
            circle(canvas, 50, 354, 12, 0xFF2A2433);
            text(canvas, "W", 50, 358, 9, 0xFFD4C8FF, true, Paint.Align.CENTER);
            text(canvas, "WMAID / architecture", 70, 354, 13, 0xFFD3CDD9, false);
            text(canvas, answered ? "RESOLVED" : "04:12", width - 38, 354, 13,
                    answered ? 0xFF8FE2B9 : 0xFFFFC56B, true, Paint.Align.RIGHT);

            text(canvas, "Other sessions", 24, 430, 16, ink, true);
            sessionRow(canvas, 24, 452, width - 48, "C", "Codex", "Vestro / execution",
                    "Usage limit — retry later", Color.rgb(202, 69, 76));
            sessionRow(canvas, 24, 530, width - 48, "✓", "Claude Code", "Air Router / verification",
                    "Checks complete", Color.rgb(36, 131, 85));

            float buttonY = Math.min(height - 76, 642);
            secondaryButton.set(24, buttonY, width - 24, buttonY + 48);
            button(canvas, secondaryButton, "Run demo again", 0xFFEDE8FF, purple);
            text(canvas, "Standalone watch connected over local Wi-Fi",
                    width / 2, buttonY + 70, 11, muted, false, Paint.Align.CENTER);
        }

        private void drawDetail(Canvas canvas, float width, float height) {
            text(canvas, "‹  All sessions", 24, 38, 14, purple, true);
            pill(canvas, 24, 72, 112, 25, "CLAUDE CODE", purple, 0x146442D6);
            text(canvas, "Decision needed", 24, 132, 31, ink, true);
            text(canvas, "WMAID / architecture", 24, 160, 14, muted, false);

            card(canvas, 18, 194, width - 36, 230, 24, surface);
            text(canvas, "QUESTION", 38, 225, 11, purple, true);
            paragraph(canvas,
                    "Approve the Firebase relay architecture and continue building the Wear OS flow?",
                    38, 260, width - 76, 20, ink, 29);
            line(canvas, 38, 354, width - 38, 354, 0xFFE6E0EB, 1);
            text(canvas, "Suggested answer", 38, 385, 12, muted, false);
            text(canvas, "Approve and continue", 38, 408, 15, ink, true);

            primaryButton.set(24, 458, width - 24, 514);
            button(canvas, primaryButton, "Approve and continue", purple, Color.WHITE);
            secondaryButton.set(24, 528, width - 24, 580);
            button(canvas, secondaryButton, "Open Claude on phone", 0xFFEDE8FF, purple);
            text(canvas, "The watch can send the short answer directly.",
                    width / 2, Math.min(height - 26, 620), 12, muted, false, Paint.Align.CENTER);
        }

        private void drawProvider(Canvas canvas, float width, float height) {
            card(canvas, 18, 18, width - 36, height - 36, 30, 0xFF1D1925);
            pill(canvas, 38, 42, 104, 25, "HANDOFF", 0xFFD4C8FF, 0x246442D6);
            text(canvas, "Claude", 38, 108, 32, Color.WHITE, true);
            text(canvas, "Remote session", 38, 136, 14, 0xFFAAA2B4, false);
            circle(canvas, width / 2, 238, 48, 0xFF2A2433);
            text(canvas, "↗", width / 2, 251, 34, 0xFFD4C8FF, true, Paint.Align.CENTER);
            text(canvas, "Exact chat handoff", width / 2, 321, 22, Color.WHITE, true, Paint.Align.CENTER);
            paragraphCentered(canvas,
                    "The production adapter opens the provider’s verified session URL. This emulator uses a safe simulated chat.",
                    width / 2, 353, width - 80, 14, 0xFFBBB4C4, 21);
            primaryButton.set(38, height - 94, width - 38, height - 42);
            button(canvas, primaryButton, "Return to WMAID", purple, Color.WHITE);
        }

        private void sessionRow(
                Canvas canvas,
                float x,
                float y,
                float width,
                String icon,
                String provider,
                String workspace,
                String status,
                int statusColor
        ) {
            card(canvas, x, y, width, 65, 18, surface);
            circle(canvas, x + 27, y + 31, 16, 0xFFF0ECF4);
            text(canvas, icon, x + 27, y + 36, 11, purple, true, Paint.Align.CENTER);
            text(canvas, provider, x + 52, y + 25, 14, ink, true);
            text(canvas, workspace, x + 52, y + 45, 11, muted, false);
            text(canvas, status, x + width - 14, y + 35, 11, statusColor, true, Paint.Align.RIGHT);
        }

        private void brand(Canvas canvas, float x, float y) {
            circle(canvas, x + 10, y - 4, 10, purple);
            circle(canvas, x + 10, y - 4, 4, Color.WHITE);
            text(canvas, "WMAID", x + 29, y, 17, ink, true);
            text(canvas, "WATCH MY AI DOING", x + 29, y + 16, 8, muted, true);
        }

        private void connector(Canvas canvas, float x1, float y1, float x2, float y2) {
            line(canvas, x1, y1, x2, y2, 0xFFE0DAE6, 2);
        }

        private void paragraph(Canvas canvas, String value, float x, float y, float width,
                               float size, int color, float lineHeight) {
            List<String> lines = wrap(value, width, size);
            for (int index = 0; index < lines.size(); index++) {
                text(canvas, lines.get(index), x, y + index * lineHeight, size, color, false);
            }
        }

        private void paragraphCentered(Canvas canvas, String value, float centerX, float y,
                                       float width, float size, int color, float lineHeight) {
            List<String> lines = wrap(value, width, size);
            for (int index = 0; index < lines.size(); index++) {
                text(canvas, lines.get(index), centerX, y + index * lineHeight,
                        size, color, false, Paint.Align.CENTER);
            }
        }

        private List<String> wrap(String value, float width, float size) {
            paint.setTextSize(size);
            paint.setTypeface(Typeface.create("sans", Typeface.NORMAL));
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : value.split(" ")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (paint.measureText(candidate) > width && line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            if (line.length() > 0) {
                lines.add(line.toString());
            }
            return lines;
        }

        private void card(Canvas canvas, float x, float y, float width, float height,
                          float radius, int color) {
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(x, y, x + width, y + height, radius, radius, paint);
        }

        private void button(Canvas canvas, RectF rect, String label, int background, int foreground) {
            paint.setColor(background);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(rect, 18, 18, paint);
            text(canvas, label, rect.centerX(), rect.centerY() + 5,
                    15, foreground, true, Paint.Align.CENTER);
        }

        private void pill(Canvas canvas, float x, float y, float width, float height,
                          String label, int foreground, int background) {
            paint.setColor(background);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(x, y, x + width, y + height, height / 2, height / 2, paint);
            text(canvas, label, x + width / 2, y + height / 2 + 4,
                    10, foreground, true, Paint.Align.CENTER);
        }

        private void circle(Canvas canvas, float x, float y, float radius, int color) {
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, radius, paint);
        }

        private void line(Canvas canvas, float x1, float y1, float x2, float y2,
                          int color, float width) {
            paint.setColor(color);
            paint.setStrokeWidth(width);
            canvas.drawLine(x1, y1, x2, y2, paint);
        }

        private void text(Canvas canvas, String value, float x, float y,
                          float size, int color, boolean bold) {
            text(canvas, value, x, y, size, color, bold, Paint.Align.LEFT);
        }

        private void text(Canvas canvas, String value, float x, float y,
                          float size, int color, boolean bold, Paint.Align align) {
            paint.setColor(color);
            paint.setTextSize(size);
            paint.setTextAlign(align);
            paint.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText(value, x, y, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP) {
                return true;
            }
            float x = event.getX() / density;
            float y = event.getY() / density;
            performClick();
            if (state == Screen.SIGN_IN && primaryButton.contains(x, y)) {
                connect();
            } else if (state == Screen.DASHBOARD && heroCard.contains(x, y)) {
                state = Screen.DETAIL;
                invalidate();
            } else if (state == Screen.DASHBOARD && secondaryButton.contains(x, y)) {
                notified = false;
                repository.resetDemo(() -> { }, this::showError);
            } else if (state == Screen.DETAIL && primaryButton.contains(x, y)) {
                AlertItem item = firstWaiting(alerts);
                if (item != null) {
                    repository.reply(item.id, "Approve and continue",
                            "Samsung phone emulator", this::showError);
                    state = Screen.DASHBOARD;
                    invalidate();
                }
            } else if (state == Screen.DETAIL && secondaryButton.contains(x, y)) {
                state = Screen.PROVIDER;
                invalidate();
            } else if (state == Screen.PROVIDER && primaryButton.contains(x, y)) {
                state = Screen.DASHBOARD;
                invalidate();
            }
            return true;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        private void showError(String value) {
            error = value;
            state = Screen.SIGN_IN;
            invalidate();
        }
    }
}
