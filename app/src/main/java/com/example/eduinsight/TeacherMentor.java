package com.example.eduinsight;

public class TeacherMentor {
    private String id;
    private String name;
    private String email;
    private String mobile;
    private String department; // This will contain both departments and subjects as formatted string

    public TeacherMentor(String id, String name, String email, String mobile, String department) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.department = department;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getMobile() { return mobile; }
    public String getDepartment() { return department; }
}
