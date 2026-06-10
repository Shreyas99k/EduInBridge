package com.example.eduinsight;

public class Doubt {
    private int id;
    private String studentName;
    private String teacherName;
    private String subject;
    private String description;
    private String status;
    private String solution;
    private String solutionImage;
    private String solutionAudio;
    private int rating;

    public Doubt(int id, String studentName, String teacherName, String subject, String description, String status, String solution, String solutionImage, String solutionAudio, int rating) {
        this.id = id;
        this.studentName = studentName;
        this.teacherName = teacherName;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.solution = solution;
        this.solutionImage = solutionImage;
        this.solutionAudio = solutionAudio;
        this.rating = rating;
    }

    public Doubt(int id, String studentName, String teacherName, String subject, String description, String status, String solution, String solutionImage, String solutionAudio) {
        this(id, studentName, teacherName, subject, description, status, solution, solutionImage, solutionAudio, 0);
    }

    public int getId() { return id; }
    public String getName() { return studentName; }
    public String getTeacherName() { return teacherName; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getSolution() { return solution; }
    public String getSolutionImage() { return solutionImage; }
    public String getSolutionAudio() { return solutionAudio; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
}