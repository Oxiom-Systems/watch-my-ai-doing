package com.oxiomsystems.wmaid.core;

import com.google.firebase.firestore.DocumentSnapshot;

public final class AlertItem {
    public final String id;
    public final String provider;
    public final String workspace;
    public final String title;
    public final String question;
    public final String suggestedReply;
    public final String status;
    public final String severity;
    public final String reply;
    public final String answeredBy;
    public final boolean handoffRequested;
    public final long createdAt;

    public AlertItem(
            String id,
            String provider,
            String workspace,
            String title,
            String question,
            String suggestedReply,
            String status,
            String severity,
            String reply,
            String answeredBy,
            boolean handoffRequested,
            long createdAt
    ) {
        this.id = id;
        this.provider = provider;
        this.workspace = workspace;
        this.title = title;
        this.question = question;
        this.suggestedReply = suggestedReply;
        this.status = status;
        this.severity = severity;
        this.reply = reply;
        this.answeredBy = answeredBy;
        this.handoffRequested = handoffRequested;
        this.createdAt = createdAt;
    }

    public boolean isWaiting() {
        return "waiting".equals(status);
    }

    public boolean isAnswered() {
        return "answered".equals(status);
    }

    public static AlertItem from(DocumentSnapshot document) {
        return new AlertItem(
                document.getId(),
                value(document, "provider"),
                value(document, "workspace"),
                value(document, "title"),
                value(document, "question"),
                value(document, "suggestedReply"),
                value(document, "status"),
                value(document, "severity"),
                value(document, "reply"),
                value(document, "answeredBy"),
                Boolean.TRUE.equals(document.getBoolean("handoffRequested")),
                document.getLong("createdAt") == null ? 0L : document.getLong("createdAt")
        );
    }

    private static String value(DocumentSnapshot document, String key) {
        String value = document.getString(key);
        return value == null ? "" : value;
    }
}
