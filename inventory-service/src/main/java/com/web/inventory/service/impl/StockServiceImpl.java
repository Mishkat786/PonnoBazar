package com.web.inventory.service.impl;

import com.web.inventory.client.ProductServiceClient;
import com.web.inventory.dto.StockDTO;
import com.web.inventory.exception.InternalServerErrorExceptionHandler;
import com.web.inventory.exception.NotFoundException;
import com.web.inventory.exception.RecordNotUpdateException;
import com.web.inventory.model.Stock;
import com.web.inventory.repository.StockRepository;
import com.web.inventory.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;


@Service
public class StockServiceImpl implements StockService {

    private static final Logger logger = LoggerFactory.getLogger(StockServiceImpl.class);

    private StockRepository stockRepository;
    private ProductServiceClient productServiceClient;
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public StockServiceImpl(StockRepository stockRepository,
                            KafkaTemplate<String, String> kafkaTemplate,
                            ProductServiceClient productServiceClient) {
        this.stockRepository = stockRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.productServiceClient = productServiceClient;
    }

    @Override
    public boolean createStock(StockDTO stockDTO) {
        // check product available
        boolean checkProductExists = productServiceClient.checkProduct(stockDTO.getProductCode());
        if (checkProductExists) {
            // check if product stock already available
            Stock existedStock = stockRepository.findStockByProductCode(stockDTO.getProductCode());
            if (existedStock == null) {
                Stock newStock = new Stock();
                newStock.setProductCode(stockDTO.getProductCode());
                newStock.setCreatedDate(new Date());
                newStock.setQuantity(stockDTO.getQuantity());
                //save new stock
                stockRepository.save(newStock);
                // check saved successfully
                Stock savedStock = getStock(stockDTO.getProductCode());
                if (savedStock.getProductCode().equals(stockDTO.getProductCode())) {
                    return true;
                }
                throw new InternalServerErrorExceptionHandler("Can't create product stock.");
            }
            throw new InternalServerErrorExceptionHandler("Stock already added. Please update the stock.");
        }
        throw new InternalServerErrorExceptionHandler("Can't fetch product.");
    }

    @Override
    public List<Stock> getAllStock() {
        List<Stock> getAllStock = stockRepository.findAll();
        if (getAllStock.size() > 0) {
            return getAllStock;
        }
        throw new NotFoundException("Stock list not found.");
    }

    @Override
    public Stock getStock(String productCode) {
        Stock stock = stockRepository.findStockByProductCode(productCode);
        if (stock != null) {
            return stock;
        }
        throw new NotFoundException("Could not found stock.");
    }

    @Override
    public boolean isStockAvailable(String productCode, Integer quantity) {
        Stock stock = getStock(productCode);
        if (stock.getQuantity() > 0 && quantity< stock.getQuantity()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean updateStock(StockDTO stockDTO) {
        boolean checkProductExists = productServiceClient.checkProduct(stockDTO.getProductCode());
        if (checkProductExists) {
            Stock stock = getStock(stockDTO.getProductCode());
            if (stock != null) {
                stock.setQuantity(stockDTO.getQuantity());
                stock.setUpdatedDate(new Date());
                stockRepository.save(stock);
                Stock updatedStock = getStock(stockDTO.getProductCode());
                if (updatedStock.getProductCode().equals(stockDTO.getProductCode())) {
                    return true;
                }
                throw new InternalServerErrorExceptionHandler("Can't update stock");
            }
            throw new InternalServerErrorExceptionHandler("Internal server error");
        }
        throw new InternalServerErrorExceptionHandler("Can't fetch product.");
    }

    @KafkaListener(
            topics = "order",
            groupId = "groupId"
    )
    @Override
    public boolean updateStockAfterPurchase(Object order) {
        System.out.println(order.toString());
        logger.info("-------------------------Producing inventory message--------------------------");
        kafkaTemplate.send("inventory", "successfully-purchased");
//        boolean checkProductExists = productServiceClient.checkProduct(productCode);
//        if (checkProductExists) {
//            Stock stock = getStock(productCode);
//            if (stock != null) {
//                boolean isStockAvailable = isStockAvailable(productCode, quantity);
//                if (isStockAvailable) {
//                    stock.setProductCode(productCode);
//                    int updatedQuantity = stockQuantityCalculation(stock, quantity);
//                    stock.setQuantity(updatedQuantity);
//                    stock.setUpdatedDate(new Date());
//                    stockRepository.save(stock);
//                    Stock updatedStock = getStock(productCode);
//                    if (updatedStock.getProductCode().equals(productCode)) {
//                        return true;
//                    }
//                }
//                throw new InternalServerErrorExceptionHandler("Can't update stock");
//            }
//            throw new InternalServerErrorExceptionHandler("Internal server error");
//        }
//        throw new InternalServerErrorExceptionHandler("Can't fetch product.");
        return false;
    }

    private int stockQuantityCalculation(Stock stock, int quantity) {
        int currentStock = stock.getQuantity();
        if (currentStock > quantity) {
            int updatedStock = currentStock - quantity;
            return updatedStock;
        }
        throw new RecordNotUpdateException("Product stock cannot updated");
    }

    @Override
    public boolean deleteStock(String productCode) {
        Stock stock = getStock(productCode);
        if (stock != null) {
            stockRepository.delete(stock);
            Stock deletedStock = getStock(productCode);
            if (deletedStock == null) {
                return true;
            }
            throw new InternalServerErrorExceptionHandler("Can't delete stock");
        }
        throw new InternalServerErrorExceptionHandler("Internal server error");
    }

}
