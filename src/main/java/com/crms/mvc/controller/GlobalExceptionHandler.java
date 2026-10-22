package com.crms.mvc.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNotFound(NoSuchElementException ex, Model model) {
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Resource not found");
        return "error/404";
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleBadRequest(IllegalStateException ex, Model model) {
        model.addAttribute("errorCode", "400");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/default";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
        model.addAttribute("errorDetail", ex.getMessage());
        return "error/500";
    }
}

