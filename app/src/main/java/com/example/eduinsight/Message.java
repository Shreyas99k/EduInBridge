package com.example.eduinsight;

public class Message {
    private String message;
    private String imageUrl;
    private String senderType; // "student" or "teacher"
    private String createdAt;

    public Message(String message, String imageUrl, String senderType, String createdAt) {
        this.message = message;
        this.imageUrl = imageUrl;
        this.senderType = senderType;
        this.createdAt = createdAt;
    }

    public String getMessage() { return message; }
    public String getImageUrl() { return imageUrl; }
    public String getSenderType() { return senderType; }
    public String getCreatedAt() { return createdAt; }
}