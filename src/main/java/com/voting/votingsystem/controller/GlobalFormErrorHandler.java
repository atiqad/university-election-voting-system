package com.voting.votingsystem.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Converts common "400 Bad Request" form binding errors into user-friendly feedback
 * (instead of a Whitelabel error page).
 */
@ControllerAdvice(annotations = Controller.class)
public class GlobalFormErrorHandler {

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            BindException.class
    })
    public String handleBadFormSubmission(Exception ex,
                                          HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) throws Exception {
        String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
        String method = request.getMethod() == null ? "" : request.getMethod();

        // Only intercept our election form POSTs; leave API behavior alone.
        if ("POST".equalsIgnoreCase(method) && uri.startsWith("/admin/elections")) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Invalid election form submission. Please re-check title and date/time fields and try again."
            );

            // POST /admin/elections -> create form
            if ("/admin/elections".equals(uri)) {
                return "redirect:/admin/elections/new";
            }
            // If the form posted back to /new (e.g., missing/empty action attribute), keep the user on the create form.
            if ("/admin/elections/new".equals(uri)) {
                return "redirect:/admin/elections/new";
            }

            // POST /admin/elections/{id} -> edit form
            // Redirect back to /admin/elections/{id}/edit when we can infer the id from the URI.
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String maybeId = parts[parts.length - 1];
                if (maybeId.matches("\\d+")) {
                    return "redirect:/admin/elections/" + maybeId + "/edit";
                }
            }

            return "redirect:/admin/elections";
        }

        // Default: let Spring handle it (will likely show Whitelabel / error page).
        throw ex;
    }
}
