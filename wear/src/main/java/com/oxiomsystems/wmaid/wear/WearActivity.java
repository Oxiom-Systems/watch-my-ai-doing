package com.oxiomsystems.wmaid.wear;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import com.oxiomsystems.wmaid.core.AlertItem;
import com.oxiomsystems.wmaid.core.WmaidRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WearActivity extends Activity {
    private WmaidRepository repository;
    private WatchView watchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new WmaidRepository(this);
        watchView = new WatchView();
        setContentView(watchView);
    }

    @Override
    protected void onDestroy() {
        repository.close();
        super.onDestroy();
    }

    private void connect() {
        watchView.screen = Screen.CONNECTING;
        watchView.invalidate();
        repository.connect(() -> repository.observe(items -> {
            watchView.alerts = items;
            AlertItem waiting = waiting(items);
            watchView.screen = waiting == null
                    ? Screen.RESOLVED
                    : waiting.handoffRequested ? Screen.HANDOFF : Screen.ALERT;
            watchView.invalidate();
        }, watchView::showError), watchView::showError);
    }

    private static AlertItem waiting(List<AlertItem> items) {
        for (AlertItem item : items) {
            if (item.isWaiting()) {
                return item;
            }
        }
        return null;
    }

    private enum Screen {
        SIGN_IN,
        CONNECTING,
        ALERT,
        RESOLVED,
        HANDOFF
    }

    private final class WatchView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF primary = new RectF();
        private final RectF secondary = new RectF();
        private final int purple = Color.rgb(200, 179, 253);
        private final int purpleStrong = Color.rgb(100, 66, 214);
        private final int white = Color.rgb(250, 248, 252);
        private final int muted = Color.rgb(181, 173, 191);
        private final int amber = Color.rgb(255, 186, 91);
        private float sx;
        private float sy;
        private Screen screen = Screen.SIGN_IN;
        private List<AlertItem> alerts = new ArrayList<>();
        private String error = "";

        WatchView() {
            super(WearActivity.this);
            setBackgroundColor(Color.rgb(9, 8, 12));
            setContentDescription("WMAID standalone watch showcase");
        }

        @Override
        protected void onDraw(Canvas raw) {
            super.onDraw(raw);
            sx = getWidth() / 450f;
            sy = getHeight() / 450f;
            raw.save();
            raw.scale(sx, sy);
            drawRim(raw);
            if (screen == Screen.SIGN_IN || screen == Screen.CONNECTING) {
                drawSignIn(raw);
            } else if (screen == Screen.ALERT) {
                drawAlert(raw);
            } else if (screen == Screen.RESOLVED) {
                drawResolved(raw);
            } else {
                drawHandoff(raw);
            }
            raw.restore();
        }

        private void drawRim(Canvas canvas) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(0xFF282330);
            canvas.drawCircle(225, 225, 216, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawSignIn(Canvas canvas) {
            text(canvas, "10:09", 225, 36, 12, muted, false, Paint.Align.CENTER);
            circle(canvas, 225, 99, 34, 0xFF211B2D);
            circle(canvas, 225, 99, 17, purpleStrong);
            circle(canvas, 225, 99, 6, white);
            text(canvas, "WMAID", 225, 155, 14, purple, true, Paint.Align.CENTER);
            text(canvas, "Your AI is waiting", 225, 191, 24, white, true, Paint.Align.CENTER);
            centeredParagraph(canvas,
                    "Connect this watch directly to the local WMAID relay.",
                    225, 221, 300, 14, muted, 19);
            pill(canvas, 154, 276, 142, 28, "⌁  WI-FI DIRECT", purple, 0xFF211B2D);
            primary.set(85, 326, 365, 382);
            button(canvas, primary, screen == Screen.CONNECTING ? "Connecting…" : "Connect demo",
                    purpleStrong, white);
            text(canvas, "No phone required", 225, 411, 12, muted, false, Paint.Align.CENTER);
            if (!error.isEmpty()) {
                text(canvas, "Relay unavailable", 225, 434, 11,
                        Color.rgb(255, 124, 135), true, Paint.Align.CENTER);
            }
        }

        private void drawAlert(Canvas canvas) {
            AlertItem item = waiting(alerts);
            text(canvas, "10:09", 225, 31, 11, muted, false, Paint.Align.CENTER);
            pill(canvas, 148, 48, 154, 29,
                    item == null
                            ? "●  CLAUDE CODE"
                            : "●  " + item.provider.toUpperCase(Locale.ROOT),
                    amber, 0xFF2A2119);
            text(canvas, item == null ? "Decision needed" : item.title,
                    225, 119, 23, white, true, Paint.Align.CENTER);
            text(canvas, "WMAID / architecture", 225, 143, 12, muted, false, Paint.Align.CENTER);
            centeredParagraph(canvas,
                    item == null
                            ? "Approve the Firebase relay architecture and continue?"
                            : item.question,
                    225, 179, 330, 15, white, 21);
            line(canvas, 106, 257, 344, 257, 0xFF302A37, 1);
            text(canvas, "SUGGESTED", 225, 281, 10, purple, true, Paint.Align.CENTER);
            text(canvas, "Approve and continue", 225, 305, 15, white, true, Paint.Align.CENTER);
            primary.set(78, 326, 372, 378);
            button(canvas, primary, "✓  Approve", purpleStrong, white);
            secondary.set(103, 389, 347, 427);
            outlineButton(canvas, secondary, "Open on phone", purple);
        }

        private void drawResolved(Canvas canvas) {
            text(canvas, "10:09", 225, 34, 11, muted, false, Paint.Align.CENTER);
            circle(canvas, 225, 124, 49, 0xFF163127);
            circle(canvas, 225, 124, 33, 0xFF218457);
            text(canvas, "✓", 225, 136, 30, white, true, Paint.Align.CENTER);
            text(canvas, "Answer sent", 225, 210, 27, white, true, Paint.Align.CENTER);
            centeredParagraph(canvas,
                    "Claude can continue. The phone has the same resolved state.",
                    225, 243, 310, 14, muted, 20);
            pill(canvas, 119, 316, 212, 34,
                    "✓  APPROVED FROM WATCH", 0xFF8FE2B9, 0xFF163127);
            primary.set(100, 374, 350, 420);
            outlineButton(canvas, primary, "Back to sessions", purple);
        }

        private void drawHandoff(Canvas canvas) {
            text(canvas, "10:09", 225, 34, 11, muted, false, Paint.Align.CENTER);
            circle(canvas, 225, 117, 42, 0xFF211B2D);
            text(canvas, "↗", 225, 130, 29, purple, true, Paint.Align.CENTER);
            text(canvas, "Sent to phone", 225, 197, 25, white, true, Paint.Align.CENTER);
            centeredParagraph(canvas,
                    "Tap the WMAID notification on your phone to open the exact session.",
                    225, 232, 310, 14, muted, 20);
            pill(canvas, 134, 315, 182, 34, "PHONE HANDOFF READY",
                    amber, 0xFF2A2119);
            primary.set(102, 374, 348, 420);
            outlineButton(canvas, primary, "Back", purple);
        }

        private void centeredParagraph(Canvas canvas, String value, float x, float y,
                                       float width, float size, int color, float lineHeight) {
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
            for (int index = 0; index < lines.size(); index++) {
                text(canvas, lines.get(index), x, y + index * lineHeight,
                        size, color, false, Paint.Align.CENTER);
            }
        }

        private void button(Canvas canvas, RectF rect, String label, int background, int foreground) {
            paint.setColor(background);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(rect, 26, 26, paint);
            text(canvas, label, rect.centerX(), rect.centerY() + 6,
                    16, foreground, true, Paint.Align.CENTER);
        }

        private void outlineButton(Canvas canvas, RectF rect, String label, int foreground) {
            paint.setColor(0xFF2A2433);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(rect, 22, 22, paint);
            text(canvas, label, rect.centerX(), rect.centerY() + 5,
                    13, foreground, true, Paint.Align.CENTER);
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

        private void text(Canvas canvas, String value, float x, float y, float size,
                          int color, boolean bold, Paint.Align align) {
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
            float x = event.getX() / sx;
            float y = event.getY() / sy;
            performClick();
            if (screen == Screen.SIGN_IN && primary.contains(x, y)) {
                connect();
            } else if (screen == Screen.ALERT && primary.contains(x, y)) {
                AlertItem item = waiting(alerts);
                if (item != null) {
                    repository.reply(item.id, "Approve and continue",
                            "Galaxy Watch Ultra emulator", this::showError);
                }
            } else if (screen == Screen.ALERT && secondary.contains(x, y)) {
                AlertItem item = waiting(alerts);
                if (item != null) {
                    repository.requestHandoff(item.id, this::showError);
                }
                screen = Screen.HANDOFF;
                invalidate();
            } else if ((screen == Screen.RESOLVED || screen == Screen.HANDOFF)
                    && primary.contains(x, y)) {
                screen = waiting(alerts) == null ? Screen.RESOLVED : Screen.ALERT;
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
            screen = Screen.SIGN_IN;
            invalidate();
        }
    }
}
