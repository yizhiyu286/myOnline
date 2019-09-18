package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;

public interface ManageService {
    public List<BaseCatalog1> getCatalog1();

    public List<BaseCatalog2> getCatalog2(String catalog1Id);

    public List<BaseCatalog3> getCatalog3(String catalog2Id);

    public List<BaseAttrInfo> getAttrList(String catalog3Id);

    void saveAttrInfo(BaseAttrInfo attrForm);

    List<BaseAttrValue> getAttrValueList(String attrId);

    List<SpuInfo> spuList(String catalog3Id);

    List<BaseSaleAttr> getBaseSaleAttr();

    void saveSpuInfo(SpuInfo spuForm);

    List<SpuSaleAttr> getSaleAttrBySpuId(String spuId);

    List<SpuImage> getImageList(String spuId);

    void saveSkuInfo(SkuInfo skuInfo);

    SkuInfo getSkuInfo(String skuId);

    List<SpuSaleAttr> getSaleAttrBySpuIdAndSkuId(String spuId, String skuId);

    String getSkuAttrValueMapping(String spuId);

    List<SkuInfo> listSkuInfo(String spuId);

    List<BaseAttrInfo> getAttrList(List<String> attrValueIdList);
}
