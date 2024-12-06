package com.senai.controledeacesso;

public class AQV {
    User user;

    public AQV(User user) {
        this.user = user;
    }

    public void allowAccess(){

    }

    public String toString() {
        return user.toString();
    }
}

