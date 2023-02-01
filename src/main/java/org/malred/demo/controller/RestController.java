package org.malred.demo.controller;

import org.malred.mvcframework.annotations.iAutowired;
import org.malred.mvcframework.annotations.iRequestMapping;
import org.malred.mvcframework.annotations.iRestController;
import org.malred.demo.pojo.User;
import org.malred.demo.service.DemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@iRestController
@iRequestMapping("/rest")
public class RestController {
    @iAutowired
    private DemoService demoService;

    @iRequestMapping("/user")
    public User query1(String name) {
        return demoService.user(name);
    }

    @iRequestMapping("/name")
    public Map query(HttpServletRequest request, HttpServletResponse response, String name) {
        return demoService.get(name);
    }
}
