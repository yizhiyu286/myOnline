package com.atguigu.gmall.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
public class GmallUserWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(GmallUserWebApplication.class, args);
	}

}
