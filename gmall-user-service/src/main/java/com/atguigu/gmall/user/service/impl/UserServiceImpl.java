package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;
    @Autowired
    UserAddressMapper userAddressMapper;
    @Autowired
    private RedisUtil redisUtil;
    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;


    @Override
    public List<UserInfo> getUserInfoListAll() {
        List<UserInfo> userInfos =  userMapper.selectAll();

        return userInfos;
    }

    @Override
    public void addUser(UserInfo userInfo) {
        userMapper.insertSelective(userInfo);
    }

    @Override
    public void updateUser(UserInfo userInfo) {
        userMapper.updateByPrimaryKeySelective(userInfo);
    }

    @Override
    public void updateUserByName(String name, UserInfo userInfo) {

        Example example = new Example(UserInfo.class);
        example.createCriteria().andEqualTo("name",name);

        userMapper.updateByExampleSelective(userInfo,example);

    }

    @Override
    public void delUserById(String id) {
        userMapper.deleteByPrimaryKey(id);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        //将密码加密
        String passwd = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        userInfo.setPasswd(passwd);
        //查数据库
        UserInfo one = userMapper.selectOne(userInfo);
        //判断该对象是否存在
        if(one != null){
            //存在
            Jedis jedis = redisUtil.getJedis();
            //拼接key
            String userKey = userKey_prefix+one.getId()+userinfoKey_suffix;
            String jsonString = JSON.toJSONString(one);
            //存入缓存
            jedis.setex(userKey,userKey_timeOut,jsonString);
            jedis.close();
            //返回完整的用户信息
            return one;
        }

        return null;
    }

    @Override
    public Boolean verify(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String userKey = userKey_prefix+userId+userinfoKey_suffix;
        Boolean exists = jedis.exists(userKey);
        if(exists){
            //延长时效
            jedis.expire(userKey,userKey_timeOut);
        }
        jedis.close();
        return exists;
    }

    @Override
    public List<UserAddress> getAddressById(String userId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        List<UserAddress> list = userAddressMapper.select(userAddress);
        return list;
    }

    @Override
    public UserInfo getUserById(String id) {
        return userMapper.selectByPrimaryKey(id);
    }
}
