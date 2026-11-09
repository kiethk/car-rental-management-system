package com.crms.mvc.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatus(ResponseStatusException ex, HttpServletResponse response, Model model) {
        response.setStatus(ex.getStatusCode().value());
        model.addAttribute("errorCode", String.valueOf(ex.getStatusCode().value()));
        model.addAttribute("errorMessage", ex.getReason() != null ? ex.getReason() : "Không tìm thấy dữ liệu");
        return ex.getStatusCode().value() == 404 ? "screens/customer/error/404/index" : "screens/customer/error/default/index";
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoSuchElementException ex, Model model) {
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Không tìm thấy dữ liệu");
        return "screens/customer/error/404/index";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResourceFound(NoResourceFoundException ex, Model model) {
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorMessage", "Không tìm thấy: " + ex.getResourcePath());
        return "screens/customer/error/404/index";
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(RuntimeException ex, Model model) {
        model.addAttribute("errorCode", "400");
        model.addAttribute("errorMessage", ex.getMessage());
        return "screens/customer/error/default/index";
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleDataAccess(DataAccessException ex, Model model) {
        model.addAttribute("errorCode", "503");
        model.addAttribute("errorMessage", "Dịch vụ tạm thời gián đoạn. Vui lòng thử lại sau.");
        return "screens/customer/error/default/index";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException(Exception ex, Model model) {
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorMessage", "Đã có lỗi không mong muốn. Vui lòng thử lại sau.");
        model.addAttribute("errorDetail", ex.getMessage());
        return "screens/customer/error/500/index";
    }
}
