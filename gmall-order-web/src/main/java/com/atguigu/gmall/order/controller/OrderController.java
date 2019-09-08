package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.dubbo.config.annotation.Reference;
@RestController
public class OrderController {

    @Reference
    UserService userService;

    @GetMapping("trade")
    public UserInfo trade(@RequestParam("userid") String userid){
        UserInfo userInfo = userService.getUserById(userid);

        return  userInfo;
    }
}
