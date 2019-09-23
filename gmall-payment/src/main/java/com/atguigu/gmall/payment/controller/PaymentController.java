package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.enums.PaymentStatus;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;
    @Reference
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;
    @GetMapping("index")
    @LoginRequire
    public String index(HttpServletRequest request){
        //获取订单id
        String orderId = request.getParameter("orderId");
        //根据订单id查询订单信息
        OrderInfo orderInfo = orderService.getOrderInfoById(orderId);
        //根据返回的订单信息,设置返回信息
        request.setAttribute("orderId",orderId);
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        //返回
        return "index";
    }

    @PostMapping("alipay/submit")
    @ResponseBody
    public String submit(HttpServletRequest request, HttpServletResponse response){
        //获取订单id
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfoById(orderId);
        //
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();

        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);
        long currentTimeMillis = System.currentTimeMillis();
        String outTradeNo="ATGUIGU-"+orderId+"-"+currentTimeMillis;
        String productNo="FAST_INSTANT_TRADE_PAY";
        BigDecimal totalAmount = orderInfo.getTotalAmount();
        String subject=orderInfo.getTradeBody();
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("out_trade_no",outTradeNo);
        jsonObject.put("product_code",productNo);
        jsonObject.put("total_amount",totalAmount);
        jsonObject.put("subject",subject);
        alipayRequest.setBizContent(jsonObject.toJSONString());

        //组织参数
        String submitHtml="";
        try {
            submitHtml = alipayClient.pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");

        //2  把提交操作保存起来

        PaymentInfo paymentInfo = new PaymentInfo();

        paymentInfo.setOrderId(orderId);
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOutTradeNo(outTradeNo);
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setSubject(subject);
        paymentInfo.setTotalAmount(totalAmount);

        paymentService.savePaymentInfo(paymentInfo);

        return submitHtml;
    }
}
