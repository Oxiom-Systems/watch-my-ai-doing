package com.oxiomsystems.wmaid.core;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Emulator-first data boundary shared by the phone and standalone watch apps.
 *
 * The deliberately non-secret options below identify the local demo project.
 * Production builds will initialize Firebase from injected release configuration.
 */
public final class WmaidRepository {
    public interface ReadyCallback {
        void onReady();
    }

    public interface ErrorCallback {
        void onError(String message);
    }

    public interface AlertsCallback {
        void onAlerts(List<AlertItem> alerts);
    }

    private static final String DEMO_PROJECT = "demo-watch-my-ai-doing";
    private static final String EMULATOR_HOST = "10.0.2.2";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final CollectionReference alerts;
    private ListenerRegistration registration;

    public WmaidRepository(Context context) {
        FirebaseApp app;
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setProjectId(DEMO_PROJECT)
                    .setApplicationId("1:1234567890:android:wmaid-demo")
                    .setApiKey("demo-api-key")
                    .build();
            app = FirebaseApp.initializeApp(context, options, "wmaid-demo");
        } else {
            app = FirebaseApp.getApps(context).get(0);
        }

        auth = FirebaseAuth.getInstance(app);
        firestore = FirebaseFirestore.getInstance(app);
        auth.useEmulator(EMULATOR_HOST, 9099);
        firestore.useEmulator(EMULATOR_HOST, 8080);
        alerts = firestore.collection("accounts")
                .document("showcase")
                .collection("alerts");
    }

    public void connect(ReadyCallback ready, ErrorCallback error) {
        if (auth.getCurrentUser() != null) {
            seedDemo(ready, error);
            return;
        }

        auth.signInAnonymously()
                .addOnSuccessListener(result -> seedDemo(ready, error))
                .addOnFailureListener(failure -> error.onError(message(failure)));
    }

    public void observe(AlertsCallback callback, ErrorCallback error) {
        if (registration != null) {
            registration.remove();
        }
        registration = alerts.addSnapshotListener((snapshot, failure) -> {
            if (failure != null) {
                error.onError(message(failure));
                return;
            }
            List<AlertItem> items = new ArrayList<>();
            if (snapshot != null) {
                for (DocumentSnapshot document : snapshot.getDocuments()) {
                    items.add(AlertItem.from(document));
                }
            }
            items.sort(Comparator.comparingLong((AlertItem item) -> item.createdAt).reversed());
            callback.onAlerts(items);
        });
    }

    public void reply(String alertId, String reply, String answeredBy, ErrorCallback error) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", "answered");
        update.put("reply", reply);
        update.put("answeredBy", answeredBy);
        update.put("answeredAt", System.currentTimeMillis());
        alerts.document(alertId).set(update, SetOptions.merge())
                .addOnFailureListener(failure -> error.onError(message(failure)));
    }

    public void requestHandoff(String alertId, ErrorCallback error) {
        Map<String, Object> update = new HashMap<>();
        update.put("handoffRequested", true);
        update.put("handoffRequestedAt", System.currentTimeMillis());
        alerts.document(alertId).set(update, SetOptions.merge())
                .addOnFailureListener(failure -> error.onError(message(failure)));
    }

    public void resetDemo(ReadyCallback ready, ErrorCallback error) {
        seedAlert("claude-plan-review", claudeAlert(), true, ready, error);
    }

    public void close() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void seedDemo(ReadyCallback ready, ErrorCallback error) {
        alerts.document("claude-plan-review").get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        alerts.document("claude-plan-review").set(claudeAlert())
                                .continueWithTask(ignored ->
                                        alerts.document("codex-overload").set(codexAlert()))
                                .continueWithTask(ignored ->
                                        alerts.document("claude-complete").set(completedAlert()))
                                .addOnSuccessListener(ignored -> ready.onReady())
                                .addOnFailureListener(failure -> error.onError(message(failure)));
                    } else {
                        ready.onReady();
                    }
                })
                .addOnFailureListener(failure -> error.onError(message(failure)));
    }

    private void seedAlert(
            String id,
            Map<String, Object> values,
            boolean merge,
            ReadyCallback ready,
            ErrorCallback error
    ) {
        alerts.document(id).set(values, SetOptions.merge())
                .addOnSuccessListener(ignored -> ready.onReady())
                .addOnFailureListener(failure -> error.onError(message(failure)));
    }

    private static Map<String, Object> claudeAlert() {
        Map<String, Object> values = new HashMap<>();
        values.put("provider", "Claude Code");
        values.put("workspace", "WMAID / architecture");
        values.put("title", "Claude needs a decision");
        values.put("question", "Approve the Firebase relay architecture and continue building the Wear OS flow?");
        values.put("suggestedReply", "Approve and continue");
        values.put("status", "waiting");
        values.put("severity", "attention");
        values.put("reply", "");
        values.put("answeredBy", "");
        values.put("handoffRequested", false);
        values.put("createdAt", 300L);
        return values;
    }

    private static Map<String, Object> codexAlert() {
        Map<String, Object> values = new HashMap<>();
        values.put("provider", "Codex");
        values.put("workspace", "Vestro / execution");
        values.put("title", "Codex paused");
        values.put("question", "The session hit a temporary usage limit. Retry when capacity is available?");
        values.put("suggestedReply", "Retry");
        values.put("status", "blocked");
        values.put("severity", "error");
        values.put("reply", "");
        values.put("answeredBy", "");
        values.put("handoffRequested", false);
        values.put("createdAt", 200L);
        return values;
    }

    private static Map<String, Object> completedAlert() {
        Map<String, Object> values = new HashMap<>();
        values.put("provider", "Claude Code");
        values.put("workspace", "Air Router / verification");
        values.put("title", "Verification complete");
        values.put("question", "All emulator checks passed.");
        values.put("suggestedReply", "");
        values.put("status", "complete");
        values.put("severity", "success");
        values.put("reply", "");
        values.put("answeredBy", "Mac watcher");
        values.put("handoffRequested", false);
        values.put("createdAt", 100L);
        return values;
    }

    private static String message(Exception failure) {
        String value = failure.getMessage();
        return value == null ? failure.getClass().getSimpleName() : value;
    }
}
