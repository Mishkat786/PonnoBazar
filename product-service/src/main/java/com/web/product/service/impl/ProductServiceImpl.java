package com.web.product.service.impl;

import com.web.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.web.product.model.Product;
import com.web.product.repository.ProductRepository;
import com.web.product.service.ProductService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
	
	private final ProductRepository productRepository;

	public ProductServiceImpl(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Override
	public boolean createProduct(Product product) {
		product.setIsActive(true);
		productRepository.save(product);
		Product saveProduct = getProduct(product.getProductCode());
		if (saveProduct.getProductCode().equals(product.getProductCode())) {
			return true;
		}
		return false;
	}

	@Override
	public Product getProduct(String productCode) {
		Product getProduct = productRepository.findByProductCode(productCode);
		if (getProduct != null && getProduct.getIsActive()) {
			return getProduct;
		}
		return null;
	}

	@Override
	public List<Product> getProducts(int page, int size) {
		Pageable paging = PageRequest.of(page, size);
		Page<Product> getPagedProducts = productRepository.findAll(paging);
		if (getPagedProducts.hasContent()) {
			List<Product> products = getPagedProducts.getContent()
										.stream().filter(x -> x.getIsActive())
										.collect(Collectors.toList());
			return products;
		}
		return new ArrayList<Product>();
	}

	@Override
	public boolean updateProduct(String productCode, Product product) {
		Product existingProduct = getProduct(productCode);
		if (existingProduct != null) {
			if (!product.getProductCode().isEmpty()) {
				productRepository.save(product);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean deleteProduct(String productCode) {
		Product existingProduct = getProduct(productCode);
		if (existingProduct != null) {
			existingProduct.setIsActive(false);
			productRepository.save(existingProduct);
			return true;
		}
		return false;
	}
}
