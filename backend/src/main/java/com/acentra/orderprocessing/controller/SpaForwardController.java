package com.acentra.orderprocessing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping(value = {"/", "/dashboard", "/orders", "/dlq"})
    public String forwardSpa() {
        return "forward:/index.html";
    }
}
