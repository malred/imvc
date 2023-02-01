package org.malred.demo.pojo;

import java.util.HashMap;
import java.util.Map;

/**
 * @author malguy-wang sir
 * @create ---
 */
public class R {
    public static Map Ok(Object obj) {
        Map res = new HashMap<String, Object>();
        res.put("status", 200);
        res.put("data", obj);
        return res;
    }
}
