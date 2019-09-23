package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotation.LoginRequire;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.service.CartInfoSevice;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.WebConst;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;

@Controller
public class CartController {

    @Reference
    private CartInfoSevice cartInfoSevice;

    @PostMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        //获取用户id
        String userId = (String)request.getAttribute("userId");
        String skuId = request.getParameter("skuId");
        String num = request.getParameter("num");
        //判断是否为空
        if(StringUtils.isEmpty(userId)){
            //先判断Cookie中是否已经存在临时id
            String userId_temp = CookieUtil.getCookieValue(request, "userId_temp", false);
            if(StringUtils.isEmpty(userId_temp)){
                //为空说明用户没有登录,给用户设置一个临时id
                userId = UUID.randomUUID().toString();
                CookieUtil.setCookie(request,response,"userId_temp", userId , WebConst.COOKIE_MAXAGE,false);
            }else{
                //不为空说明这不是该临时id第一次添加购物车
                userId = userId_temp;
            }
        }
        //调用方法将该商品添加到数据库和缓存中
        CartInfo cartInfo = cartInfoSevice.addToCart(userId, skuId, Integer.parseInt(num));
        //放到请求域中返回
        request.setAttribute("cartInfo",cartInfo);
        request.setAttribute("num",num);
        return "success";
    }
    @GetMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request){
        //先取userId判断当前用户是否登录
        String userId = (String)request.getAttribute("userId");
        if(userId!=null){   //有登录
            List<CartInfo> cartList=null;   //如果登录前（未登录）时，存在临时购物车 ，要考虑合并
            String userTmpId=CookieUtil.getCookieValue(request, "userId_temp", false); //取临时id
            if(userTmpId!=null){
                List<CartInfo> cartTempList =  cartInfoSevice.cartList(  userTmpId);  //如果有临时id ，查是否有临时购物车
                if( cartTempList!=null&&cartTempList.size()>0){
                    cartList=  cartInfoSevice.mergeCartList(userId,userTmpId); // 如果有临时购物车 ，那么进行合并 ，并且获得合并后的购物车列表
                }
            }
            if(cartList==null||cartList.size()==0){
                cartList =  cartInfoSevice.cartList(  userId);  //如果不需要合并 ，再取登录后的购物车
            }
            request.setAttribute("cartList",cartList);
        }else {   //未登录 直接取临时购物车
            String userTmpId=CookieUtil.getCookieValue(request, "userId_temp", false);
            if(userTmpId!=null) {
                List<CartInfo> cartTempList = cartInfoSevice.cartList(userTmpId);
                request.setAttribute("cartList",cartTempList);
            }

        }
        return "cartList";
    }

    @PostMapping("checkCart")
    @LoginRequire(autoRedirect = false)
    @ResponseBody
    public void checkCart(HttpServletRequest request,@RequestParam("isChecked") String isChecked,@RequestParam("skuId") String skuId){
        //获取当前用户id，判断是登录状态还是未登录状态
        String userId = (String)request.getAttribute("userId");
        //判断
        if(StringUtils.isEmpty(userId)){
            //为空说明是未登录用户，从cookie中获取用户的临时id
            userId = CookieUtil.getCookieValue(request, "userId_temp",false);
        }
        //调用方法更改购物车的勾选状态
        cartInfoSevice.checkCart(userId,isChecked,skuId);
    }
}
