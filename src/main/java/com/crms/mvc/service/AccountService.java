package com.crms.mvc.service;

import com.crms.mvc.dto.AccountProfileDTO;
import com.crms.mvc.dto.SessionAccount;

public interface AccountService {
    AccountProfileDTO getProfile(SessionAccount account);

    SessionAccount update(SessionAccount account, AccountProfileDTO form);
}
