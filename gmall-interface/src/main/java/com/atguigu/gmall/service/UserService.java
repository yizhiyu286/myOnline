package com.atguigu.gmall.service;


import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

public interface UserService {
    List<UserInfo> getUserInfoListAll();

    UserInfo getUserById(String id);

    void addUser(UserInfo userInfo);

    void updateUser(UserInfo userInfo);

    void updateUserByName(String name,UserInfo userInfo);

    void delUserById(String id);

    UserInfo login(UserInfo userInfo);

    Boolean verify(String userId);

    List<UserAddress> getAddressById(String userId);
}
