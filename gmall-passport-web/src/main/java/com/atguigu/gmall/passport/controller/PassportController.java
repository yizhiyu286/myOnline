package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.passport.util.JwtUtil;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    private UserService userService;

    @Value("${token.key}")
    private String tokenKey;
    @RequestMapping("index")
    public String index(HttpServletRequest request){
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo, HttpServletRequest request){
        //根据传入的用户名和密码去数据库进行校验
        if(userInfo != null){
            UserInfo userInfoExsit = userService.login(userInfo);
            //判断结果
            if(userInfoExsit == null){
                return "fail";
            }else{
                //获取nginx传过来的IP地址
                String addr = request.getHeader("X-forwarded-for");
                //将用户的信息放入map中
                Map map = new HashMap<>();
                map.put("userId",userInfoExsit.getId());
                map.put("nickName",userInfoExsit.getNickName());
                //加密token
                String token = JwtUtil.encode(tokenKey, map, addr);
                System.out.println(token);
                return token;
            }

        }
        return "fail";
    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        //获取token值和IP值
        String token = request.getParameter("token");
        String currentIp = request.getParameter("currentIp");
        //进行解码
        Map<String, Object> map = JwtUtil.decode(token, tokenKey, currentIp);
        if(map != null){
            //获取用户id再从缓存中检查
            String userId = (String)map.get("userId");
            //调用方法检查
            Boolean bool = userService.verify(userId);
            //判断
            if(bool){
                return "success";
            }
        }
        return "fail";
    }
}
