package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.SkuLsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class SkuLsController {

    @Reference
    private SkuLsService skuLsService;

    @Reference
    private ManageService manageService;

    @GetMapping("list.html")
    public String selectSkuList(SkuLsParams skuLsParams,Model model){
        skuLsParams.setPageSize(2);
        //调用方法查询sku信息
        SkuLsResult bySkuLsParams = skuLsService.getBySkuLsParams(skuLsParams);
        //封装返回
        model.addAttribute("skuInfoList",bySkuLsParams);
        //根据查询出的平台属性值id查询对应的平台属性以及平台属性值
        List<BaseAttrInfo> attrList = manageService.getAttrList(bySkuLsParams.getAttrValueIdList());
        model.addAttribute("attrList",attrList);
        //返回url
        String urlParam = makeUrlParam(skuLsParams);
        //创建集合存放面包屑文字
        List<BaseAttrValue> baseAttrValueList = new ArrayList<>();
        //排除已被选中的平台属性值id
        if(skuLsParams.getValueId() != null && skuLsParams.getValueId().length>0){
            for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
                BaseAttrInfo baseAttrInfo =  iterator.next();
                //获取平台属性值集合
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                for (BaseAttrValue baseAttrValue : attrValueList) {
                    //用平台属性值的id匹配从页面接收到的平台属性值id的数组
                    for (String valueId : skuLsParams.getValueId()) {
                        //一旦匹配，移除当前平台属性,并修改路径
                        if(valueId.equals(baseAttrValue.getId())){
                            iterator.remove();
                            //创建平台属性值对象存放
                            BaseAttrValue bav = new BaseAttrValue();
                            bav.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                            //调用方法获取路径
                            String param = makeUrlParam(skuLsParams, valueId);
                            bav.setUrlParam(param);
                            //添加到面包屑集合中
                            baseAttrValueList.add(bav);
                        }
                    }
                }

            }
        }

        model.addAttribute("baseAttrValueList",baseAttrValueList);
        model.addAttribute("urlParam",urlParam);
        model.addAttribute("totalPages",bySkuLsParams.getTotalPages());
        model.addAttribute("pageNo",skuLsParams.getPageNo());
        //关键词
        model.addAttribute("keyword",skuLsParams.getKeyword());
        return "list";
    }
    //拼接url的方法
    private String makeUrlParam(SkuLsParams skuLsParams,String...bavIds){
        String url = "";
        //判断是否有keyword
        if(skuLsParams.getKeyword() != null){
            //不为空，拼接keyword
            url += "keyword="+skuLsParams.getKeyword();
        }else{
            //说明通过三级分类进入，拼接三级分类
            url += "catalog3Id="+skuLsParams.getCatalog3Id();
        }
        //拼接平台属性值id
        if(skuLsParams.getValueId() != null && skuLsParams.getValueId().length>0){
            String[] valueIds = skuLsParams.getValueId();
            //循环拼接
            for (int i = 0; i < valueIds.length; i++) {
                 String valueId = valueIds[i];
                 if(bavIds != null && bavIds.length>0){
                     String bavId = bavIds[0];
                     if(bavId.equals(valueId)){
                         continue;
                     }
                 }
                if(url.length()>0){
                    url += "&";
                }
                url += "valueId="+valueId;
            }
        }
        return url;
    }
}
