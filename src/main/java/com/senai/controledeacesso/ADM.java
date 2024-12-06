package com.senai.controledeacesso;

public class ADM {
    User user;

    public ADM(User user) {
        this.user = user;
    }

    public void allowAccess(){

    }

    public String toString() {
        return user.toString();
    }
}
