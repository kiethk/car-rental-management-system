package com.crms.mvc.service;

import com.crms.mvc.dto.LoginIdentityDTO;
import com.crms.mvc.exception.AccountLockedException;

public interface AuthService {
    /**
     * @throws AccountLockedException when the credentials are valid but the account is disabled
     */
    LoginIdentityDTO login(String email, String password);
}
