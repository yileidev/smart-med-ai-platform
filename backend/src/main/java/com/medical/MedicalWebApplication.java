package com.medical;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MedicalWebApplication {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("医疗分诊系统后端正在启动...");
        System.out.println("========================================");
        
        try {
            SpringApplication.run(MedicalWebApplication.class, args);
            System.out.println("\n✅ 应用启动成功!");
        } catch (Exception e) {
            System.err.println("\n❌ 应用启动失败!");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}