package com.senai.controledeacesso;

public class User {
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

    public void checkData(){
        System.out.println(this);
    }

    public String toString() {
        return ID + "\t\t\t" + name + "\t\t\t" + identifier + "\t\t\t" + password + "\t\t\t";
    }
}