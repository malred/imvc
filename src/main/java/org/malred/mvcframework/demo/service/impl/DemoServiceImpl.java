package org.malred.mvcframework.demo.service.impl;

import org.malred.mvcframework.annotations.iService;
import org.malred.mvcframework.demo.service.DemoService;
@iService("demoService")
public class DemoServiceImpl implements DemoService {
    @Override
    public String get(String name) {
        System.out.println(name);
        return name;
    }
}
