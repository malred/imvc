package org.malred.mvcframework.servlet;

import org.apache.commons.lang3.StringUtils;
import org.malred.mvcframework.annotations.iAutowired;
import org.malred.mvcframework.annotations.iController;
import org.malred.mvcframework.annotations.iRequestMapping;
import org.malred.mvcframework.annotations.iService;
import org.malred.mvcframework.pojo.Handler;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DispatcherServlet extends HttpServlet {
    // 配置文件对象
    private Properties properties = new Properties();
    // ioc容器
    private Map<String, Object> ioc = new HashMap<String, Object>();
    // handlerMapping
//    private Map<String, Method> handlerMapping = new HashMap<>(); // 存储url和method的映射
    private Map<String, Handler> handlerMapping = new HashMap<>(); // 存储url和method的映射

    @Override
    public void init(ServletConfig config) throws ServletException {
        // 1,加载配置文件 springmvc.properties (在web.xml配置)
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        doLoadConfig(contextConfigLocation);
        // 2,扫描相关类和注解
        doScan(properties.getProperty("scanPackage"));
        // 3,初始化bean对象(实现ioc容器,基于注解)
        doInstance();
        // 4,实现依赖注入
        doAutowired();
        // 5,构造一个handlerMapping处理器映射器,将配置好的url和method建立映射关系
        initHandlerMapping();
        System.out.println("==========>> imvc 初始化完成 <<==========");
        // 6,等待请求进入,处理请求
    }

    // 初始化映射器(关键!!!将url和方法建立关联)
    private void initHandlerMapping() {
        if (ioc.isEmpty()) return;
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            // 获取ioc中当前遍历的对象的class类型
            Class<?> aClass = entry.getValue().getClass();
            if (!aClass.isAnnotationPresent(iController.class)) continue;
            String baseUrl = "";
            if (aClass.isAnnotationPresent(iRequestMapping.class)) {
                iRequestMapping annotation = aClass.getAnnotation(iRequestMapping.class);
                baseUrl = annotation.value(); // /demo
            }
            // 获取方法
            Method[] methods = aClass.getMethods();
            for (Method method : methods) {
                // 方法没有标识,不处理
                if (!method.isAnnotationPresent(iRequestMapping.class)) continue;
                iRequestMapping annotation = method.getAnnotation(iRequestMapping.class);
                String methodUrl = annotation.value();
                String url = baseUrl + methodUrl;
                // 把method所有信息和url封装为一个handler
                Handler handler = new Handler(entry.getValue(), method, Pattern.compile(url));
                // 方法的参数信息
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    if (parameter.getType() == HttpServletRequest.class ||
                            parameter.getType() == HttpServletResponse.class) {
                        // 如果是request和response对象,则参数名称改为HttpServletRequest,HttpServletResponse
                        handler.getParamIndexMapping().put(parameter.getType().getSimpleName(), i);
                    } else {
                        handler.getParamIndexMapping().put(parameter.getName(), i);
                    }
                }
                // 建立url和method的映射关系
//                handlerMapping.put(url, method);
                handlerMapping.put(url, handler);
            }
        }
    }

    // 实现依赖注入
    private void doAutowired() {
        if (ioc.isEmpty()) return;
        // 有对象再进行依赖注入
        // 遍历ioc中所有对象,如果有autowired注解,就注入
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            // 获取bean对象中的字段信息
            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();
            // 遍历
            for (int i = 0; i < declaredFields.length; i++) {
                Field declaredField = declaredFields[i];
                if (!declaredField.isAnnotationPresent(iAutowired.class)) {
                    continue;
                }
                // 有autowired注解
                iAutowired annotation = declaredField.getAnnotation(iAutowired.class);
                String beanName = annotation.value(); // 需要注入的bean的id
                if ("".equals(beanName.trim())) {
                    // 如果没有指定要注入的bean,就根据当前字段类型注入(接口注入)
                    beanName = declaredField.getType().getName();
                }
                // 赋值
                declaredField.setAccessible(true); // 强制访问
                try {
                    // set(当前字段或其所在类的全类名,值)
                    // ioc.get(beanName) beanName是要注入的bean的id
                    declaredField.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ioc容器
    // 基于classNames里的全类名,通过反射,实现对象创建和管理
    private void doInstance() {
        if (classNames.size() == 0) return;
        try {
            for (int i = 0; i < classNames.size(); i++) {
                String className = classNames.get(i);
                // 反射
                Class<?> aClass = Class.forName(className); // 根据全类名加载类
                // 区分controller和*service
                if (aClass.isAnnotationPresent(iController.class)) {
                    // 直接拿首字母小写做为id,保存到ioc
                    String simpleName = aClass.getSimpleName();// DemoController
                    String lowerFirstSimpleName = lowerFirst(simpleName); // demoController
                    Object o = aClass.newInstance();
                    ioc.put(lowerFirstSimpleName, o);
                } else if (aClass.isAnnotationPresent(iService.class)) {
                    iService annotation = aClass.getAnnotation(iService.class);
                    // 获取注解的value值
                    String beanName = annotation.value();
                    // 不为空
                    if (!"".equals(beanName.trim())) {
                        // 如果指定了id,就以指定的为准
                        ioc.put(beanName, aClass.newInstance());
                    } else {
                        // 没有指定,就首字母小写做id
                        beanName = lowerFirst(aClass.getSimpleName());
                        ioc.put(beanName, aClass.newInstance());
                    }
                    // service层往往有接口,此时再以接口名为id,放一份对象到ioc,便于后期根据接口类型注入
                    Class<?>[] interfaces = aClass.getInterfaces();
                    for (int j = 0; j < interfaces.length; j++) {
                        Class<?> anInterface = interfaces[j];
                        // 以接口的全类名作为id放入
                        ioc.put(anInterface.getName(), aClass.newInstance());
                    }
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 缓存扫描到的类的全限定类名
    public List<String> classNames = new ArrayList<String>();

    // 扫描类
    // scanPackage: org.malred.demo -> 磁盘上的文件夹(File)
    private void doScan(String scanPackage) {
        try {
            String scanPackagePath = Thread.currentThread().getContextClassLoader().getResource("").getPath()
                    + scanPackage.replaceAll("\\.", "/");
            File pack = new File(scanPackagePath);
            File[] files = pack.listFiles();
            for (File file : files) {
                // 如果是包,要扫描包下的文件
                if (file.isDirectory()) {
                    // 递归
                    doScan(scanPackage + "." + file.getName()); // org.malred.demo.controller
                } else if (file.getName().endsWith(".class")) {
                    // 如果是java类,就获取全类名
                    String className =
                            scanPackage + "." + file.getName().replaceAll(".class", "");
                    classNames.add(className);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 加载配置文件
    private void doLoadConfig(String contextConfigLocation) {
        // 加载资源流,从中读取配置文件
        InputStream resourceAsStream =
                this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(resourceAsStream);// 加载
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 首字母转小写
    public String lowerFirst(String str) {
        char[] chars = str.toCharArray();
        // 如果char[0]是大写
        if ('A' <= chars[0] && chars[0] <= 'Z') {
            chars[0] += 32;
        }
        return String.valueOf(chars);
    }

    // 接收处理请求
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 处理请求,根据url找到method,进行调用
        // 获取url
//        String requestURI = req.getRequestURI();
//        Method method = handlerMapping.get(requestURI); // 获取方法
        // 反射调用,需要传入对象和参数,无法调用
//        method.invoke();
        // 根据url获取handler
        Handler handler = getHandler(req);
        if (handler == null) {
            resp.getWriter().write("404 not found");
            return;
        }
        // 参数绑定
        // 获取所有参数的类型
        Class<?>[] parameterTypes = handler.getMethod().getParameterTypes();
        // 根据参数类型数组长度,创建一个参数数组
        Object[] paramValues = new Object[parameterTypes.length];
        // 获取req的参数
        Map<String, String[]> parameterMap = req.getParameterMap();
        // 遍历req所有的参数
        for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
            // name=1&name=2
            String value = StringUtils.join(param.getValue(), ","); // 1,2
            // 填充数据
            if (!handler.getParamIndexMapping().containsKey(param.getKey())) {
                continue;
            }
            // 方法形参有该参数,找到他的索引位置,把参数值放入对应位置
            Integer index = handler.getParamIndexMapping().get(param.getKey());
            paramValues[index] = value; // 前端传来的参数放入args
        }
        // 存request和response(如果方法参数有)
        if (handler.getParamIndexMapping().containsKey(HttpServletRequest.class.getSimpleName())) {
            int requestIndex =
                    handler.getParamIndexMapping().get(HttpServletRequest.class.getSimpleName());
            paramValues[requestIndex] = req;
        }
        if (handler.getParamIndexMapping().containsKey(HttpServletResponse.class.getSimpleName())) {
            int responseIndex =
                    handler.getParamIndexMapping().get(HttpServletResponse.class.getSimpleName());
            paramValues[responseIndex] = resp;
        }
        try {
            handler.getMethod().invoke(handler.getController(), paramValues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private Handler getHandler(HttpServletRequest req) {
        if (handlerMapping.isEmpty()) return null;
        String url = req.getRequestURI();
        for (Handler handler : handlerMapping.values()) {
            Matcher matcher = handler.getPattern().matcher(url);
            if (!matcher.matches()) {
                continue;
            }
            return handler;
        }
        return null;
    }
}
