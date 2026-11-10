package com.crms.mvc.exception;

/** Credentials are correct, but the account has been disabled by an administrator. */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException() {
        super("Tài khoản đã bị khóa");
    }
}
