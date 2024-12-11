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
        String formattedTable = String.format(
                "+----------------------+-------------------+------------------------+-------------------+\n" +
                        "| %-20s | %-17s | %-22s | %-17s |\n" +
                        "+----------------------+-------------------+------------------------+-------------------+\n" +
                        "| %-20s | %-17s | %-22s | %-17s |\n" +
                        "+----------------------+-------------------+------------------------+-------------------+",
                "ID", "NOME", "NÚMERO DE MATRÍCULA", "SENHA",
                ID, name, identifier, password
        );
        return formattedTable;
    }
}