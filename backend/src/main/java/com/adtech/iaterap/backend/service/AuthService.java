package com.adtech.iaterap.backend.service;

import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public String login(String username, String password) {

        // JWT logic will come here later
        return "Login Successful for " + username;
    }
}
