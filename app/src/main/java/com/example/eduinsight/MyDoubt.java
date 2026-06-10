package com.example.eduinsight;

public class MyDoubt {
    private int id;
    private String subject;
    private String description;
    private String status;
    private String answer;
    private String solImage;
    private String solAudio;

    public MyDoubt(int id, String subject, String description, String status, String answer, String solImage, String solAudio) {
        this.id = id;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.answer = answer;
        this.solImage = solImage;
        this.solAudio = solAudio;
    }

    // Getters used by the Adapter and Search Filter
    public int getId() { return id; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getAnswer() { return answer; }
    public String getSolImage() { return solImage; }
    public String getSolAudio() { return solAudio; }
}