package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.SkuLsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;
    @Reference
    private SkuLsService skuLsService;
    @GetMapping("{skuId}.html")
    public String getItem(@PathVariable("skuId") String skuId, Model model){
        //根据skuid获取sku基本信息
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        //将sku对象放入域对象中返回
        model.addAttribute("skuInfo",skuInfo);
        //根据spuId与skuId查询当前spu对应的所有销售属性值以及当前sku对应的销售属性销售属性值
        if(skuInfo != null){
            List<SpuSaleAttr> list = manageService.getSaleAttrBySpuIdAndSkuId(skuInfo.getSpuId(),skuId);
            model.addAttribute("saleList",list);
            //System.out.println(list);
            //查询当前spu下每一个sku与销售属性以及销售属性值的映射关系转换成json字符串传到页面
            String valuesSku = manageService.getSkuAttrValueMapping(skuInfo.getSpuId());
            model.addAttribute("valuesSku",valuesSku);
        }
        //调用方法将该sku的hotscore值增加
        skuLsService.incrHotscore(skuId);
        return "item";
    }
}
