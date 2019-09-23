package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.service.CartInfoSevice;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartInfoServiceImpl implements CartInfoSevice {
    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Reference
    private ManageService manageService;
    @Autowired
    private RedisUtil redisUtil;
    @Override
    public CartInfo addToCart(String userId, String skuId, Integer num) {
        //添加之前判断缓存中是否有当前用户对应的购物车数据
        loadCartCacheIfExist(userId);
        //先根据用户id和skuId查询当前用户是否在购物车加入过当前商品
        CartInfo selectCartInfo = new CartInfo();
        selectCartInfo.setSkuId(skuId);
        selectCartInfo.setUserId(userId);
        CartInfo cartInfo = cartInfoMapper.selectOne(selectCartInfo);
        //数据库操作===============================================
        //判断购物车中是否加了当前商品
        if(cartInfo!=null){
            //不为空说明当前商品已存在购物车中,进行修改操作
            cartInfo.setSkuNum(cartInfo.getSkuNum()+num);
            //设置cartInfo中的实时价格属性
            cartInfo.setSkuPrice(cartInfo.getCartPrice());
            //修改
            cartInfoMapper.updateByPrimaryKeySelective(cartInfo);
        }else{
            //为空说明没有当前的商品，进行插入操作
            //先根据skuid获取当前的商品信息
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            //设置购物车信息
            CartInfo insertCartInfo = new CartInfo();
            insertCartInfo.setUserId(userId);
            insertCartInfo.setSkuId(skuId);
            insertCartInfo.setSkuNum(num);
            insertCartInfo.setSkuName(skuInfo.getSkuName());
            insertCartInfo.setCartPrice(skuInfo.getPrice());
            insertCartInfo.setSkuPrice(skuInfo.getPrice());
            insertCartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            //插入信息
            cartInfoMapper.insertSelective(insertCartInfo);
            cartInfo = insertCartInfo;
        }
        //Redis缓存操作================================================
        //获取jedis
        Jedis jedis = redisUtil.getJedis();
        //拼接key
        String cartKey = "cart:"+userId+":info";
        //将数据转成json字符串
        String cartInfoJson = JSON.toJSONString(cartInfo);
        //将数据存入redis缓存中
        jedis.hset(cartKey,skuId,cartInfoJson);
        //设置过期时间
        jedis.expire(cartKey,60*60*24);
        jedis.close();
        //为了数据库与redis的数据一致性，每次进行添加操作之后都刷新缓存
        //loadCartCache(userId);
        return cartInfo;
    }

    @Override
    public List<CartInfo> cartList(String userId) {
        //先查缓存，缓存中有的话直接返回缓存中的
        Jedis jedis = redisUtil.getJedis();
        //拼接cart的key
        String cartKey = "cart:"+userId+":info";
        //从redis中获取数据
        List<String> cartInfoListJson = jedis.hvals(cartKey);
        //创建一个集合存放购物车列表集合
        List<CartInfo> cartInfoList = new ArrayList<>();
        //判断redis中获取的数据
        if(cartInfoListJson!=null && cartInfoListJson.size()>0){
            //说明redis中有值，取值返回
            for (String cartInfoJson : cartInfoListJson) {
                //将json字符串转成对象
                CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
                //添加到集合中
                cartInfoList.add(cartInfo);
            }
            //对购物车列表进行排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o2.getId().compareTo(o1.getId());
                }
            });
            //返回集合
            return cartInfoList;
        }else{
            //查询数据库并加载到缓存中返回
            return loadCartCache(userId);
        }

    }

    @Override
    public List<CartInfo> mergeCartList(String userId, String userId_temp) {
        //调用mapper进行合并查询
        cartInfoMapper.mergeCartList(userId,userId_temp);
        //合并完成后将数据库已经合并的临时数据删除
        CartInfo deleteCartInfo = new CartInfo();
        deleteCartInfo.setUserId(userId_temp);
        cartInfoMapper.delete(deleteCartInfo);
        //将redis中的临时数据也删除
        Jedis jedis = redisUtil.getJedis();
        jedis.del("cart:"+userId_temp+":info");
        jedis.close();
        //返回结果
        List<CartInfo> cartInfoList = loadCartCache(userId);
        return cartInfoList;
    }

    @Override
    public void checkCart(String userId, String isChecked, String skuId) {
        //获取数据之前先查缓存看是否有
        loadCartCacheIfExist(userId);
        //先从redis中获取购物车信息
        Jedis jedis = redisUtil.getJedis();
        //拼接key
        String cartKey = "cart:"+userId+":info";
        //获取当前skuid对应的商品
        String cartInfoJson = jedis.hget(cartKey, skuId);
        //将该商品转成对象
        CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
        //设置该商品的勾选状态
        cartInfo.setIsChecked(isChecked);
        //转成字符串再存进缓存中
        cartInfoJson = JSON.toJSONString(cartInfo);
        //存入
        jedis.hset(cartKey,skuId,cartInfoJson);
        //再根据该商品的勾选状态修改缓存中已勾选的购物车
        String checkedKey = "cart:"+userId+":checked";
        if("1".equals(isChecked)){
            //说明是已勾选，将该商品添加进已勾选商品的集合中
            jedis.hset(checkedKey,skuId,cartInfoJson);
            //设置该已选集合的过期时间
            jedis.expire(checkedKey,60*60*24);
        }else{
            //说明是取消勾选，将该商品从当前已选集合中删除
            jedis.hdel(checkedKey,skuId);
        }
        jedis.close();
    }

    @Override
    public List<CartInfo> getCheckedCart(String userId) {
        //拼接key
        String checkedKey = "cart:"+userId+":checked";
        //从缓存中获取
        Jedis jedis = redisUtil.getJedis();
        List<String> checkedCartJsonList = jedis.hvals(checkedKey);
        //创建一个集合存放已选的数据
        List<CartInfo> cartInfoList = new ArrayList<>();
        //遍历存放
        for (String checkedCartJson : checkedCartJsonList) {
            //转成对象
            CartInfo cartInfo = JSON.parseObject(checkedCartJson, CartInfo.class);
            //放入集合
            cartInfoList.add(cartInfo);
        }
        jedis.close();
        return cartInfoList;
    }

    private List<CartInfo> loadCartCache(String userId) {
        //首先根据用户id取数据库查询，并将实时的价格信息返回
        List<CartInfo> cartInfoList = cartInfoMapper.selectWithUserIdAndSkuPrice(userId);
        //将查询结果遍历并封装成map集合
        if(cartInfoList != null && cartInfoList.size()>0){
            Map<String,String> map = new HashMap<>();
            for (CartInfo cartInfo : cartInfoList) {
                map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
            }
            //存入redis中
            Jedis jedis = redisUtil.getJedis();
            //拼接key
            String cartKey = "cart:"+userId+":info";
            //先删除，因为hmset方法不会自动覆盖
            jedis.del(cartKey);
            jedis.hmset(cartKey,map);
            //设置过期时间
            jedis.expire(cartKey,60*60*24);
            jedis.close();
        }
        return cartInfoList;
    }

    private void loadCartCacheIfExist(String userId){
        //该方法用于判断缓存中是否有当前用户id对应的购物车数据
        Jedis jedis = redisUtil.getJedis();
        //拼接key
        String cartKey = "cart:"+userId+":info";
        //防止极端情况的出现，先将该key的剩余时间查出，如果没有则返回的是-2
        Long ttl = jedis.ttl(cartKey);
        //将该key的存在时间延长一些，避免出现刚刚判断完有然后就失效的极端情况
        jedis.expire(cartKey,ttl.intValue()+10);
        //查询是否存在
        Boolean exists = jedis.exists(cartKey);
        jedis.close();
        //判断
        if(!exists){
            //为否说明不存在
            //加载缓存
            loadCartCache(userId);
        }
    }

}
