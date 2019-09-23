package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    RedisUtil redisUtil;
    @Override
    public String getToken(String userId) {
        //获取一个随机的UUID
        String token = UUID.randomUUID().toString();
        //存入redis中
        Jedis jedis = redisUtil.getJedis();
        //拼接key
        String tokenKey = "user:"+userId+":trade_code";
        jedis.setex(tokenKey,60*15,token);
        jedis.close();
        return token;
    }

    @Override
    public Boolean verifyToken(String userId, String tradeNo) {
        //拼接key
        String tokenKey = "user:"+userId+":trade_code";
        //从redis中获取token
        Jedis jedis = redisUtil.getJedis();
        String token = jedis.get(tokenKey);
        //watch监视
        jedis.watch(tokenKey);
        //进行验证删除，防止出现并发情况下产生的同时判断成功的情况，使用reds的事务组队
        Transaction transaction = jedis.multi();
        //判断
        if(token!=null && token.equals(tradeNo)){
            //正确,删除token
            transaction.del(tokenKey);
        }
        //事务组队执行
        List<Object> exec = transaction.exec();
        //通过判断执行结果得知验证成功还是失败
        if(exec!=null&&exec.size()>0&&(Long)exec.get(0)==1L){
            //验证成功
            return true;
        }else{
            return false;
        }

    }

    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {
        //保存订单信息
        orderMapper.insertSelective(orderInfo);
        //保存订单每个商品的详细信息
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //遍历保存
        for (OrderDetail orderDetail : orderDetailList) {
            //设置订单id
            orderDetail.setOrderId(orderInfo.getId());
            //保存
            orderDetailMapper.insertSelective(orderDetail);
        }
        return orderInfo.getId();
    }

    @Override
    public OrderInfo getOrderInfoById(String orderId) {
        //先根据订单id查询订单信息
        OrderInfo orderInfo = orderMapper.selectByPrimaryKey(orderId);
        //然后根据订单id查询订单详细
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderInfo.getId());
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        //设置
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

}
