package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        //根据skuid获取对应的图片
        if(skuInfo != null){
            SkuImage skuImage = new SkuImage();
            skuImage.setSkuId(skuId);
            List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
            //将图片信息放入sku对象返回
            skuInfo.setSkuImageList(skuImageList);
        }
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
}
