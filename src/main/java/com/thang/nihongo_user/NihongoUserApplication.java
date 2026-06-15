package com.thang.nihongo_user;

import com.nihongo.security.CommonSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@Import(CommonSecurityConfig.class)
@EnableCaching
public class NihongoUserApplication {

	public static void main(String[] args) {
		SpringApplication.run(NihongoUserApplication.class, args);
	}

}
