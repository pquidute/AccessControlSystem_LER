package com.senai.controledeacesso;

import org.json.JSONArray;

import java.util.ArrayList;

public class Student {
    User user;
    String classroom;
    int delays;
    int accessId;
    ArrayList<String> arrayDelays;


    Student(User user, String classroom, int accessId) {
        this.user = user;
        this.classroom = classroom;
        this.accessId = accessId;
    }

    public void showAccessRegisters(){
        if (this.delays == 0){
            System.out.println("Você não tem atrasos!");
        }else
            System.out.println("Você tem " + this.delays +  " atrasos");
    }

    public String toString() {
        return String.format(
                "|  ID  | NOME  | NÚMERO DE MATRÍCULA | SENHA | TURMA | ATRASOS | ID ACESSO |\n " +
                "| %-5d | %-15s | %-15s | %-10s | %-10s | %-6d | %-6d |",
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
