package com.crms.mvc.service;
import com.crms.mvc.entity.Customer;
import java.util.Optional;
public interface AuthService {
    Optional<Customer> login(String email, String password);
}
