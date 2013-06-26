package com.marklogic.utilities;

import com.zaubersoftware.gnip4j.api.GnipAuthentication;

public final class InmutableGnipAuthentication implements GnipAuthentication {

    private final String username;
    private final String password;
    
    public InmutableGnipAuthentication(final String username, final String password) {
        if(username == null) {
            throw new IllegalArgumentException("The username cannot be null");
        }
        if(password == null) {
            throw new IllegalArgumentException("The password cannot be null");
        }
            
        this.username = username;
        this.password = password;
    }
    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }
}