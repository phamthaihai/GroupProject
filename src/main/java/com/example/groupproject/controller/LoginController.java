package com.example.groupproject.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for the login page and public landing redirect.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * SCR-13 is the public landing page for guests and authenticated users.
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/jobs";
    }
}
