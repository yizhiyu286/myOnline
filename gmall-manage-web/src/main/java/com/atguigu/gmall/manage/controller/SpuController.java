package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseSaleAttr;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.service.ManageService;
import com.oracle.webservices.internal.api.message.PropertySet;
import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
public class SpuController {
    @Reference
    private ManageService manageService;

    @Value("${fileServer.url}")
    private String fileUrl;

    //GET http://localhost:8082/spuList?catalog3Id=61
    @GetMapping("spuList")
    public List<SpuInfo> spuList(String catalog3Id){
        List<SpuInfo> list = manageService.spuList(catalog3Id);
        return list;
    }

    @PostMapping("fileUpload")
    public String fileUpload(MultipartFile file) throws IOException, MyException {
        String imgUrl= fileUrl;
        if(file!=null){
            System.out.println("multipartFile = " + file.getName()+"|"+file.getSize());
            String configFile = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(configFile);
            TrackerClient trackerClient=new TrackerClient();
            TrackerServer trackerServer=trackerClient.getConnection();
            StorageClient storageClient=new StorageClient(trackerServer,null);
            String filename= file.getOriginalFilename();
            String extName = StringUtils.substringAfterLast(filename, ".");

            String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);
            imgUrl=fileUrl ;
            for (int i = 0; i < upload_file.length; i++) {
                String path = upload_file[i];
                imgUrl+="/"+path;
            }

        }

        return imgUrl;

    }

    @PostMapping("baseSaleAttrList")
    public List<BaseSaleAttr> baseSaleAttrList(){
        List<BaseSaleAttr> list = manageService.getBaseSaleAttr();
        return list;
    }

    @PostMapping("saveSpuInfo")
    public String saveSpuInfo(@RequestBody SpuInfo spuForm){
        manageService.saveSpuInfo(spuForm);
        return "success";
    }
}
