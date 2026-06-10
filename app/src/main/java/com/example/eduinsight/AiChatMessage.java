package com.example.eduinsight;

public class AiChatMessage {
    private String message;
    private boolean isUser;

    public AiChatMessage(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }

    public String getMessage() { return message; }
    public boolean isUser() { return isUser; }
}
