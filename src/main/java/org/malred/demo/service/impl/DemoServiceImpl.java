package org.malred.demo.service.impl;

import org.malred.mvcframework.annotations.iService;
import org.malred.demo.pojo.R;
import org.malred.demo.pojo.User;
import org.malred.demo.service.DemoService;

import java.util.Map;

@iService("demoService")
public class DemoServiceImpl implements DemoService {
    @Override
    public Map get(String name) {
        System.out.println(name);
        return R.Ok(name);
    }

    @Override
    public User user(String name) {
        return new User(name);
    }
}
