package com.ecommerce.ecommercebackend.seeder; // Đổi lại package cho đúng

import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() == 0) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                InputStream inputStream = TypeReference.class.getResourceAsStream("/data_phone.json");
                if (inputStream == null) {
                    System.err.println("❌ Không tìm thấy file data_phone.json!");
                    return;
                }

                try {
                    JsonNode rootNode = mapper.readTree(inputStream);
                    if (rootNode.isArray()) {
                        for (JsonNode productNode : rootNode) {
                            ObjectNode objNode = (ObjectNode) productNode;

                            // 1. Xử lý trường hợp mảng cho brand và category
                            if (objNode.has("brand") && objNode.get("brand").isArray()) {
                                objNode.put("brand", objNode.get("brand").get(0).asText());
                            }
                            if (objNode.has("category") && objNode.get("category").isArray()) {
                                objNode.put("category", objNode.get("category").get(0).asText());
                            }

                            // 2. Xử lý format tiền tệ cho current_price
                            if (objNode.has("current_price") && objNode.get("current_price").isTextual()) {
                                String priceStr = objNode.get("current_price").asText();
                                // Lọc bỏ tất cả ký tự không phải là số (giữ lại 0-9)
                                String cleanedPrice = priceStr.replaceAll("[^\\d]", "");
                                if (!cleanedPrice.isEmpty()) {
                                    // Chuyển chuỗi sạch thành kiểu Long và ghi đè lại
                                    objNode.put("current_price", Long.parseLong(cleanedPrice));
                                } else {
                                    objNode.put("current_price", 0L); // Nếu rỗng hoặc "Giá liên hệ" thì set = 0
                                }
                            }

                            // 3. Xử lý format tiền tệ cho original_price (nếu file json của bạn có)
                            if (objNode.has("original_price") && objNode.get("original_price").isTextual()) {
                                String oldPriceStr = objNode.get("original_price").asText();
                                String cleanedOldPrice = oldPriceStr.replaceAll("[^\\d]", "");
                                if (!cleanedOldPrice.isEmpty()) {
                                    objNode.put("original_price", Long.parseLong(cleanedOldPrice));
                                } else {
                                    objNode.put("original_price", 0L);
                                }
                            }
                        }
                    }
                    
                    List<Product> products = mapper.convertValue(
                            rootNode, 
                            new TypeReference<List<Product>>(){}
                    );
                    
                    productRepository.saveAll(products);
                    
                    System.out.println("✅ Import data từ data_phone.json thành công! Đã clean format tiền tệ.");
                } catch (Exception e) {
                    System.err.println("❌ Quá trình import thất bại. Lỗi chi tiết: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("⚡ Bảng products đã có dữ liệu, bỏ qua bước import.");
            }
        };
    }
}