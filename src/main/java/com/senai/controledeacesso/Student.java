package com.senai.controledeacesso;

import java.util.ArrayList;

public class Student {
    User user;
    String classroom;
    int delays;
    int accessId;
    ArrayList<String> arrayDelays;


    Student(User user, String classroom) {
        this.user = user;
        this.classroom = classroom;
    }

    public void showAccessRegisters(){
        if (arrayDelays.isEmpty()){
            System.out.println("O aluno não tem atrasos registrados");
        }
        for (int i = 0; i < arrayDelays.size(); i++) {
            System.out.println(arrayDelays.get(i));
        }
    }

    public String toString() {
        return user.toString() + "\nTURMA: " + classroom + "\nATRASOS: " + delays;
    }
}
