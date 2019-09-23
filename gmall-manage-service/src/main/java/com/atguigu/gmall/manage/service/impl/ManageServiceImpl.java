package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.constant.ManageConst;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;
    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;
    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Override
    public List<BaseCatalog1> getCatalog1() {
        List<BaseCatalog1> catalog1List = baseCatalog1Mapper.selectAll();
        return catalog1List;
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        Example example = new Example(BaseCatalog2.class);
        example.createCriteria().andEqualTo("catalog1Id",catalog1Id);
        List<BaseCatalog2> catalog2List = baseCatalog2Mapper.selectByExample(example);
        return catalog2List;
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        Example example = new Example(BaseCatalog3.class);
        example.createCriteria().andEqualTo("catalog2Id",catalog2Id);
        List<BaseCatalog3> catalog3List = baseCatalog3Mapper.selectByExample(example);
        return catalog3List;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
        List<BaseAttrInfo> attrInfoList = baseAttrInfoMapper.getAttrInfoListByCatalog3Id(catalog3Id);
        return attrInfoList;
    }

    @Transactional
    @Override
    public void saveAttrInfo(BaseAttrInfo attrForm) {
        //先判断是否有id，决定是修改还是保存
        if(attrForm.getId()!=null && attrForm.getId().length()>0){
            //有id，修改
            baseAttrInfoMapper.updateByPrimaryKeySelective(attrForm);
        }else{
            //无id，添加
            //保存平台属性名
            baseAttrInfoMapper.insertSelective(attrForm);
        }

        String attrFormId = attrForm.getId();
        //不管是增加还是修改，都先删除，再添加
        Example example = new Example(BaseAttrValue.class);
        example.createCriteria().andEqualTo("attrId",attrFormId);
        baseAttrValueMapper.deleteByExample(example);
        //获取平台属性值
        List<BaseAttrValue> attrValueList = attrForm.getAttrValueList();
        System.out.println(attrValueList);
        //遍历保存平台属性值
        for (BaseAttrValue baseAttrValue : attrValueList) {
            baseAttrValue.setAttrId(attrFormId);
            baseAttrValueMapper.insertSelective(baseAttrValue);
        }
    }

    @Override
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        Example example = new Example(BaseAttrValue.class);
        example.createCriteria().andEqualTo("attrId",attrId);
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.selectByExample(example);
        return baseAttrValueList;
    }

    @Override
    public List<SpuInfo> spuList(String catalog3Id) {
        Example example = new Example(SpuInfo.class);
        example.createCriteria().andEqualTo("catalog3Id",catalog3Id);
        List<SpuInfo> spuInfos = spuInfoMapper.selectByExample(example);
        return spuInfos;
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttr() {
        List<BaseSaleAttr> baseSaleAttrs = baseSaleAttrMapper.selectAll();
        return baseSaleAttrs;
    }

    @Transactional
    @Override
    public void saveSpuInfo(SpuInfo spuForm) {
        //先保存Spu基本信息
        spuInfoMapper.insertSelective(spuForm);
        //然后保存图片信息路径
        List<SpuImage> spuImageList = spuForm.getSpuImageList();
        String spuId = spuForm.getId();
        for (SpuImage spuImage : spuImageList) {
            //设置spuid
            spuImage.setSpuId(spuId);
            //保存
            spuImageMapper.insertSelective(spuImage);
        }
        //保存销售属性
        List<SpuSaleAttr> spuSaleAttrList = spuForm.getSpuSaleAttrList();
        for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
            //设置id
            spuSaleAttr.setSpuId(spuId);
            spuSaleAttrMapper.insert(spuSaleAttr);
            //保存销售属性值
            List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
            for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                //设置
                spuSaleAttrValue.setSpuId(spuId);
                //保存
                spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
            }
        }
    }

    @Override
    public List<SpuSaleAttr> getSaleAttrBySpuId(String spuId) {
        List<SpuSaleAttr> list = spuSaleAttrMapper.getSaleAttrListBySpuId(spuId);
        return list;
    }

    @Override
    public List<SpuImage> getImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        List<SpuImage> list = spuImageMapper.select(spuImage);
        return list;
    }

    @Transactional
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        //保存sku基本信息
        //判断是保存还是修改
        if(skuInfo.getId() == null || skuInfo.getId().length() == 0)
            skuInfoMapper.insertSelective(skuInfo);
        else
            skuInfoMapper.updateByPrimaryKeySelective(skuInfo);
        //保存图片信息,先删除再保存
        SkuImage skuImage1 = new SkuImage();
        skuImage1.setSkuId(skuInfo.getId());
        skuImageMapper.delete(skuImage1);
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        for (SkuImage skuImage : skuImageList) {
            skuImage.setSkuId(skuInfo.getId());
            skuImageMapper.insertSelective(skuImage);
        }
        //保存平台属性值,先删除再保存
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuInfo.getSpuId());
        skuAttrValueMapper.delete(skuAttrValue);
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue attrValue : skuAttrValueList) {
            attrValue.setSkuId(skuInfo.getId());
            skuAttrValueMapper.insertSelective(attrValue);
        }
        //保存销售属性值,先删除再保存
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuInfo.getId());
        skuSaleAttrValueMapper.delete(skuSaleAttrValue);
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (SkuSaleAttrValue saleAttrValue : skuSaleAttrValueList) {
            saleAttrValue.setSkuId(skuInfo.getId());
            skuSaleAttrValueMapper.insertSelective(saleAttrValue);
        }
    }

    @Override
    public SkuInfo getSkuInfo(String skuId) {
        SkuInfo skuInfo = null;
        RLock lock =null;
        Jedis jedis =null;
        try {
            // 测试redis String
            jedis = redisUtil.getJedis();

            // 定义key
            String userKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            if (jedis.exists(userKey)){
                // 获取缓存中的数据
                String userJson = jedis.get(userKey);
                if (!StringUtils.isEmpty(userJson)){
                    skuInfo = JSON.parseObject(userJson, SkuInfo.class);
                    return skuInfo;
                }
            }else {
                // 创建config
                Config config = new Config();
                // redis://192.168.67.220:6379 配置文件中！
                config.useSingleServer().setAddress("redis://192.168.199.133:6379");

                RedissonClient redisson = Redisson.create(config);

                lock = redisson.getLock("my-lock");

                lock.lock(10, TimeUnit.SECONDS);
                // 从数据库查询数据
                skuInfo = getSkuInfoDB(skuId);
                // 将数据放入缓存
                // jedis.set(userKey,JSON.toJSONString(skuInfo));
                jedis.setex(userKey,ManageConst.SKUKEY_TIMEOUT,JSON.toJSONString(skuInfo));
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis!=null){
                jedis.close();
            }
            if (lock!=null){
                lock.unlock();
            }

        }

        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfo_Redis(String skuId) {
        SkuInfo skuInfo = null;
        //先从redis中查询是否有该缓存
        Jedis jedis = redisUtil.getJedis();
        //拼接前缀
        String skuIdKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
        //获取存在redis中的sku信息对象的json字符串
        String skuJson = jedis.get(skuIdKey);
        //判断是否为空
        if(!StringUtils.isEmpty((skuJson))){
            //不是空的话,判断是否为"EMPTY"转成对象返回
            if(!"EMPTY".equals(skuJson)){
                skuInfo= JSON.parseObject(skuJson, SkuInfo.class);
            }
        }else{
            //为空的话查询数据库
            //先上锁，避免出现缓存击穿的问题
            //拼接锁的前缀
            String lockKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.LOCKED_SUFFIX;
            //为了避免出现删除别人的锁的情况，给锁设置唯一的值
            String lockValue = UUID.randomUUID().toString();
            //设置锁并设置锁的过期时间
            String lockJson = jedis.set(lockKey, lockValue, "NX", "EX", 10);
            //判断返回是否为OK，如果是OK可以进行查询
            if("OK".equals(lockJson)){
                //查询数据库
                skuInfo = getSkuInfoDB(skuId);
                //将查询结果判断放入缓存，避免出现缓存穿透
                if(skuInfo == null){
                    //为空设置值为"EMPTY"
                    String emptyValue = "EMPTY";
                    //存入缓存
                    jedis.setex(skuIdKey,ManageConst.SKUKEY_TIMEOUT,emptyValue);
                }else{
                    //不为空将对象转成json字符串存入
                    String skuJsonValue = JSON.toJSONString(skuInfo);
                    jedis.setex(skuIdKey,ManageConst.SKUKEY_TIMEOUT,skuJsonValue);
                }
                //完成将锁删除
                //先判断当前的锁是否是自己的
                if(lockValue.equals(jedis.get(lockKey))){
                    jedis.del(lockKey);
                }

            }else{
                //不是OK说明有人上锁，进行自旋等待
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                skuInfo = getSkuInfo(skuId);
            }

        }
        jedis.close();
        return skuInfo;
    }

    private SkuInfo getSkuInfoDB(String skuId) {
        SkuInfo skuInfo;
        skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);

        if(skuInfo == null){
            return null;
        }
        //根据skuid获取对应的图片
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        //将图片信息放入sku对象返回
        skuInfo.setSkuImageList(skuImageList);
        //根据skuid获取对应的平台属性
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValues = skuAttrValueMapper.select(skuAttrValue);
        //将平台属性值封装进对象
        skuInfo.setSkuAttrValueList(skuAttrValues);
        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> getSaleAttrBySpuIdAndSkuId(String spuId, String skuId) {
        return spuSaleAttrMapper.getSaleAttrBySpuIdAndSkuId(spuId,skuId);
    }

    @Override
    public String getSkuAttrValueMapping(String spuId) {
        List<SkuSaleAttrValue> list = skuSaleAttrValueMapper.getSkuAttrValueMapping(spuId);
        System.out.println(list);
        //遍历集合，拼接字符串
        //字符串格式为 {110|111:33}
        String str = "";
        //创建map集合存放字符串
        Map<String,String> map = new HashMap<>();
        for(int i = 0;i<list.size();i++){
            SkuSaleAttrValue attrValue = list.get(i);
            String id = attrValue.getId();

            if(str.length()>0){
                str += "|";
            }
            str += id;
            if((i+1)==list.size() || !attrValue.getSkuId().equals(list.get(i+1).getSkuId())){
                System.out.println(i);
                //满足条件，添加到map集合中
                map.put(str,attrValue.getSkuId());
                //清空字符串
                str = "";
            }
            
        }
        System.out.println(map.size());
        //将map转化为json字符串
        String valuesSku = JSON.toJSONString(map);
        return valuesSku;
    }

    @Override
    public List<SkuInfo> listSkuInfo(String spuId) {
        //根据spuId查询对应的sku
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSpuId(spuId);
        List<SkuInfo> skuInfoList = skuInfoMapper.select(skuInfo);
        //遍历集合，将平台属性信息封装
        for (int i = 0;i < skuInfoList.size();i++) {
            SkuAttrValue skuAttrValue = new SkuAttrValue();
            skuAttrValue.setSkuId(skuInfoList.get(i).getId());
            List<SkuAttrValue> skuAttrValues = skuAttrValueMapper.select(skuAttrValue);
            skuInfoList.get(i).setSkuAttrValueList(skuAttrValues);
        }
        //返回
        return skuInfoList;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {
        //将集合转为数组
        String selectString = StringUtils.join(attrValueIdList.toArray(), ",");
        //调用方法查询
        List<BaseAttrInfo> list = baseAttrInfoMapper.getAttrInfoListByList(selectString);
        return list;
    }


}
