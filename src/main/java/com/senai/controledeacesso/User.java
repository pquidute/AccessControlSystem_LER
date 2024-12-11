package com.senai.controledeacesso;

public class User {
    public String imagePath;
    String name;
    String identifier;
    String password;
    int ID;

    public User(String name, String identifier, String password) {
        this.name = name;
        this.identifier = identifier;
        this.password = password;
    }

    public boolean Login(String identifier, String password){
        if (password.equals(this.password) && identifier.equals(this.identifier)){
            return true;
        }else
            return false;
    }

    public String toString() {
        return String.format(
                "| %-5d | %-15s | %-15s | %-10s | %-10s | %-6s | %-9s |",
                ID, name, identifier, password, "N/A", 0, "N/A"
        );
    }
}