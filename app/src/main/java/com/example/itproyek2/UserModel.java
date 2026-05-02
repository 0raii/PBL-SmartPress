package com.example.itproyek2;

public class UserModel {
    private int id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String password;
    private String photo;

    public UserModel(int id, String name, String email, String password, String phone, String role, String photo) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.role = role;
        this.photo = photo;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getPhoto() { return photo; }
}
