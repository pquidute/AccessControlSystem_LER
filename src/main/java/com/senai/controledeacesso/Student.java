package com.senai.controledeacesso;

public class Student {
    User user;
    String classroom;
    int delays;


    Student(User user, String classroom) {
        this.user = user;
        this.classroom = classroom;
    }

    public void showAccessRegisters(){

    }

    public String toString() {
        return user.toString() + "\nTURMA: " + classroom + "\t\t\tATRASOS: " + delays;
    }
}
