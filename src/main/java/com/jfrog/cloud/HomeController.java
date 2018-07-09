package com.jfrog.cloud;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;


@RestController
public class HomeController {
    @RequestMapping("/")
    public String hello() {
        return "Welcome to Artifactory version " + System.getenv("APP_VERSION") + " !";
    }
}