package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class ManageController {
    @Reference
    private ManageService manageService;

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
}
