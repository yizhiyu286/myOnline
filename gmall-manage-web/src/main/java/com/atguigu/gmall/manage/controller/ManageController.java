package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.SkuLsService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class ManageController {
    @Reference
    private ManageService manageService;
    @Reference
    private SkuLsService skuLsService;

    @PostMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1(){
        List<BaseCatalog1> catalog1List = manageService.getCatalog1();
        return catalog1List;
    }

    @PostMapping("getCatalog2")
    public List<BaseCatalog2> getCatalog2(String catalog1Id){
        List<BaseCatalog2> catalog2List = manageService.getCatalog2(catalog1Id);
        return catalog2List;
    }

    @PostMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        List<BaseCatalog3> catalog3List = manageService.getCatalog3(catalog2Id);
        return catalog3List;
    }

    @GetMapping("attrInfoList")
    public List<BaseAttrInfo> getAttrInfo(String catalog3Id){
        List<BaseAttrInfo> attrList = manageService.getAttrList(catalog3Id);
        return attrList;
    }

    @PostMapping("saveAttrInfo")
    public String saveAttrInfo(@RequestBody BaseAttrInfo attrForm){
        manageService.saveAttrInfo(attrForm);
        return "success";
    }

    @PostMapping("getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(String attrId){
        List<BaseAttrValue> baseAttrValueList =  manageService.getAttrValueList(attrId);
        return baseAttrValueList;
    }

    @GetMapping("spuSaleAttrList")
    public List<SpuSaleAttr> spuSaleAttrList(String spuId){
        List<SpuSaleAttr> list = manageService.getSaleAttrBySpuId(spuId);
        return list;
    }

    @GetMapping("spuImageList")
    public List<SpuImage> spuImageList(String spuId){
        List<SpuImage> list = manageService.getImageList(spuId);
        return list;
    }
    //上架单个sku方法
    @PostMapping("onSaleSku")
    public String onSaleSku(String skuId){
        //先根据skuid查询出对应的skuinfo
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        //将数据对拷进SkuListInfo中
        SkuListInfo skuListInfo = new SkuListInfo();
        BeanUtils.copyProperties(skuInfo,skuListInfo);
        //调用方法上架skuListInfo
        skuLsService.saveSkuInfoToEs(skuListInfo);
        return "success";
    }
    //通过spuId上架对应的多个sku
    @PostMapping("onSaleSpu")
    public String onSaleSpu(String spuId){
        //先根据spuid查询对应的多个skuid
        List<SkuInfo> skuInfoList = manageService.listSkuInfo(spuId);
        //遍历集合进行对拷
        for (SkuInfo skuInfo : skuInfoList) {
            SkuListInfo skuListInfo = new SkuListInfo();
            BeanUtils.copyProperties(skuInfo,skuListInfo);
            skuLsService.saveSkuInfoToEs(skuListInfo);
        }
        return "success";
    }
}
