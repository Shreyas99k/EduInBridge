package com.example.eduinsight;

public class MentorshipMessage {
    private int id;
    private String message;
    private String senderType; 
    private String createdAt;
    private String imageUrl;
    private String audioUrl;
    private String replyToMsg;
    private String replyToUser;
    private boolean isSaved;
    private boolean isDeleted;
    private boolean isEdited;
    private int deletedBy;

    public MentorshipMessage(int id, String message, String senderType, String createdAt, String imageUrl, String audioUrl, String replyToMsg, String replyToUser, boolean isSaved, boolean isDeleted, boolean isEdited, int deletedBy) {
        this.id = id;
        this.message = message;
        this.senderType = senderType;
        this.createdAt = createdAt;
        this.imageUrl = imageUrl;
        this.audioUrl = audioUrl;
        this.replyToMsg = replyToMsg;
        this.replyToUser = replyToUser;
        this.isSaved = isSaved;
        this.isDeleted = isDeleted;
        this.isEdited = isEdited;
        this.deletedBy = deletedBy;
    }

    public int getId() { return id; }
    public String getMessage() { return message; }
    public String getSenderType() { return senderType; }
    public String getCreatedAt() { return createdAt; }
    public String getImageUrl() { return imageUrl; }
    public String getAudioUrl() { return audioUrl; }
    public String getReplyToMsg() { return replyToMsg; }
    public String getReplyToUser() { return replyToUser; }
    public boolean isSaved() { return isSaved; }
    public boolean isDeleted() { return isDeleted; }
    public boolean isEdited() { return isEdited; }
    public int getDeletedBy() { return deletedBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MentorshipMessage that = (MentorshipMessage) o;
        return id == that.id &&
                isSaved == that.isSaved &&
                isDeleted == that.isDeleted &&
                isEdited == that.isEdited &&
                deletedBy == that.deletedBy &&
                java.util.Objects.equals(message, that.message) &&
                java.util.Objects.equals(senderType, that.senderType) &&
                java.util.Objects.equals(createdAt, that.createdAt) &&
                java.util.Objects.equals(imageUrl, that.imageUrl) &&
                java.util.Objects.equals(audioUrl, that.audioUrl) &&
                java.util.Objects.equals(replyToMsg, that.replyToMsg) &&
                java.util.Objects.equals(replyToUser, that.replyToUser);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, message, senderType, createdAt, imageUrl, audioUrl, replyToMsg, replyToUser, isSaved, isDeleted, isEdited, deletedBy);
    }
}
