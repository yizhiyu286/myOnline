package com.atguigu.gmall.cart;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallCartWebApplicationTests {

	@Test
	public void contextLoads() {
		BigDecimal bigDecimal = new BigDecimal("5488.00");
		BigDecimal bigDecimal1 = new BigDecimal(5488);
		System.out.println(bigDecimal.equals(bigDecimal1));
		System.out.println(bigDecimal1.equals(bigDecimal));
	}

}
