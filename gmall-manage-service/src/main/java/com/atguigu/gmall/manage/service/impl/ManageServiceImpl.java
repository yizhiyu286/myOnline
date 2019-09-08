package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
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
        Example example = new Example(BaseAttrInfo.class);
        example.createCriteria().andEqualTo("catalog3Id",catalog3Id);
        List<BaseAttrInfo> attrInfoList = baseAttrInfoMapper.selectByExample(example);
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
}
