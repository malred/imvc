package org.malred.demo.controller;

import org.malred.demo.pojo.User;
import org.malred.demo.service.DemoService;
import org.malred.mvcframework.annotations.iAutowired;
import org.malred.mvcframework.annotations.iController;
import org.malred.mvcframework.annotations.iRequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@iController
//@iRequestMapping("/demo")
public class DemoController {
    @iAutowired
    private DemoService demoService;

    @iRequestMapping("/name")
    public Map query(HttpServletRequest request, HttpServletResponse response, String name) {
        return demoService.get(name);
    }

    @iRequestMapping("/user")
    public User query1(String name) {
        return demoService.user(name);
    }
}
