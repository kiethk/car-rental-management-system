package com.crms.mvc.service;

import com.crms.mvc.dto.LoginIdentityDTO;

public interface AuthService {
    LoginIdentityDTO login(String email, String password);
}
