package com.senai.controledeacesso;

import org.json.JSONArray;

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
        return String.format(
                "| %-5d | %-15s | %-15s | %-10s | %-10s | %-6d | %-9d |",
                user.ID, user.name, user.identifier, user.password, classroom, delays, accessId
        );
    }

    public void addDelay(String delayDetails) {
        arrayDelays.add(delayDetails);
    }

    public JSONArray arrayDelays() {
        return null;
    }
}
