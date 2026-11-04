package com.subby.global.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * React 화면 경로로 새로고침하거나 직접 접속하면 index.html을 내려준다. 이후 경로 처리는 React Router가 한다.
 */
@Controller
public class SpaForwardController {

    @GetMapping({
            "/login", "/signup", "/forgot-password", "/reset-password",
            "/subscriptions", "/subscriptions/**", "/stats", "/settings", "/notifications", "/privacy"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
