package com.crms.mvc.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatus(ResponseStatusException ex, HttpServletResponse response, Model model) {
        response.setStatus(ex.getStatusCode().value());
        model.addAttribute("errorCode", String.valueOf(ex.getStatusCode().value()));
        model.addAttribute("errorMessage", ex.getReason() != null ? ex.getReason() : "Resource not found");
        return ex.getStatusCode().value() == 404 ? "error/404" : "error/default";
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoSuchElementException ex, Model model) {
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Resource not found");
        return "error/404";
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, Model model) {
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorMessage", "Resource not found: " + ex.getResourcePath());
        return "error/404";
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(RuntimeException ex, Model model) {
        model.addAttribute("errorCode", "400");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/default";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException(Exception ex, Model model) {
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
        model.addAttribute("errorDetail", ex.getMessage());
        return "error/500";
    }
}
