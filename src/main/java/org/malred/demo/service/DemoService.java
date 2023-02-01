package org.malred.demo.service;

import org.malred.demo.pojo.User;

import java.util.Map;

/**
 * @author malguy-wang sir
 * @create ---
 */
public interface DemoService {
    Map get(String name);

    User user(String name);
}
