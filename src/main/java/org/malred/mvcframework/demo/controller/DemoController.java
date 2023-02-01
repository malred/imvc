package org.malred.mvcframework.demo.controller;

import org.malred.mvcframework.annotations.iAutowired;
import org.malred.mvcframework.annotations.iController;
import org.malred.mvcframework.annotations.iRequestMapping;
import org.malred.mvcframework.demo.service.DemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@iController
@iRequestMapping("/demo")
public class DemoController {
    @iAutowired
    private DemoService demoService;

    @iRequestMapping("/name")
    public String query(HttpServletRequest request, HttpServletResponse response, String name) {
        return demoService.get(name);
    }
}
