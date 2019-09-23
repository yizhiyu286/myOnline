package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.enums.OrderStatus;
import com.atguigu.gmall.enums.ProcessStatus;
import com.atguigu.gmall.service.CartInfoSevice;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.HttpClientUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.dubbo.config.annotation.Reference;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Controller
public class OrderController {

    @Reference
    UserService userService;
    @Reference
    OrderService orderService;
    @Reference
    CartInfoSevice cartInfoSevice;
    @Reference
    ManageService manageService;
    @GetMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request){
       //获取当前登录的用户id
        String userId = (String)request.getAttribute("userId");
        //获取当前用户的收货相关信息
        List<UserAddress> addressList = userService.getAddressById(userId);
        //设置在请求域中返回给页面
        request.setAttribute("addressList",addressList);
        //根据用户id获取当前用户已勾选的商品
        List<CartInfo> cartInfoList = cartInfoSevice.getCheckedCart(userId);
        //遍历获取当前已勾选商品总价格
        BigDecimal totalAmount = new BigDecimal("0");
        for (CartInfo cartInfo : cartInfoList) {
            BigDecimal cartAmount = cartInfo.getSkuPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
            totalAmount = totalAmount.add(cartAmount);
        }
        //获取一个token值，防止表单重复提交
        String trade_code = orderService.getToken(userId);
        //返回给页面
        request.setAttribute("cartInfoList",cartInfoList);
        request.setAttribute("totalAmount",totalAmount);
        request.setAttribute("tradeNo",trade_code);
        return  "trade";
    }

    @PostMapping("submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo,HttpServletRequest request){
        //获取当前登录的用户id
        String userId = (String)request.getAttribute("userId");
        //获取当前页面的token值
        String tradeNo = request.getParameter("tradeNo");
        //调用方法进行验证
        Boolean isEnable = orderService.verifyToken(userId,tradeNo);
        //判断验证结果
        if(!isEnable){
            //验证失败，去往错误页面
            request.setAttribute("errMsg","页面已失效，请重新结算！");
            return "tradeFail";
        }
        //验证成功，保存订单
        //设置属性
        //订单状态，未支付
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        //订单进程状态，未支付
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        //创建日期
        orderInfo.setCreateTime(new Date());
        //过期日期
        orderInfo.setExpireTime(DateUtils.addMinutes(new Date(),15));
        //总金额
        orderInfo.sumTotalAmount();
        //用户id
        orderInfo.setUserId(userId);

        //循环设置订单中每个商品的详细信息
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            //根据skuid获取商品相关信息
            SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
            //判断商品实时价格与订单价格是否一致
            //System.out.println(orderDetail.getOrderPrice()+"=====================================");
            //System.out.println(skuInfo.getPrice());
            if(!orderDetail.getOrderPrice().equals(skuInfo.getPrice())){
                //不一致返回错误页面，订单失效
                request.setAttribute("errMsg","商品价格已发送变动请重新下单！！");
                return "tradeFail";
            }
            //默认图片路径
            orderDetail.setImgUrl(skuInfo.getSkuDefaultImg());
            //商品名称
            orderDetail.setSkuName(skuInfo.getSkuName());
        }
        //验库存
        //使用流式编程
        //创建一个存放库存不足商品的集合
        List<OrderDetail> errList = Collections.synchronizedList(new ArrayList<>());
        Stream<CompletableFuture<Boolean>> completableFutureStream = orderDetailList.stream().map(orderDetail ->
                CompletableFuture.supplyAsync(() -> checkStock(orderDetail)).whenComplete((hasStock, ex) -> {
                    if (!hasStock) {
                        errList.add(orderDetail);
                    }
                }));
        //获取completableFuttures的集合
        CompletableFuture[] completableFutures = completableFutureStream.toArray(CompletableFuture[]::new);
        //表示所有线程执行完毕
        CompletableFuture.allOf(completableFutures).join();
        //判断库存是否充足
        if(errList.size()>0){
            //创建一个字符串存放库存不足的商品信息
            StringBuilder stringBuilder = new StringBuilder();
            //说明有商品库存不足
            for (OrderDetail detail : errList) {
                stringBuilder.append("商品："+detail.getSkuName()+"库存不足!!");
            }
            //去往错误页面
            request.setAttribute("errMsg",stringBuilder.toString());
            return "tradeFail";
        }
        //调用方法保存订单
        String orderId = orderService.saveOrder(orderInfo);
        //重定向到支付页面
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }

    private Boolean checkStock(OrderDetail orderDetail) {
        //使用httpclient远程调用库存系统
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + orderDetail.getSkuId() + "&num=" + orderDetail.getSkuNum());
        //判断结果
        if("1".equals(result)){
            //1表示库存充足
            return true;
        }else{
            //库存不足
            return false;
        }
    }
}
