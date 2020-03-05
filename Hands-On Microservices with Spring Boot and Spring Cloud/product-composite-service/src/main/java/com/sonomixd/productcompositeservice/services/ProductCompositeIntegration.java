package com.sonomixd.productcompositeservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonomixd.api.core.product.Product;
import com.sonomixd.api.core.product.ProductService;
import com.sonomixd.api.core.recommendation.Recommendation;
import com.sonomixd.api.core.recommendation.RecommendationService;
import com.sonomixd.api.core.review.Review;
import com.sonomixd.api.core.review.ReviewService;
import com.sonomixd.util.exception.InvalidInputException;
import com.sonomixd.util.exception.NotFoundException;
import com.sonomixd.util.http.HttpErrorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    @Autowired
    public ProductCompositeIntegration(
        RestTemplate restTemplate,
        ObjectMapper mapper,

        /** Inject the configuration values we use to set up the URLs for the three core services into the constructor */
        @Value("${app.product-service.host}") String productServiceHost,
        @Value("${app.product-service.port}") int    productServicePort,

        @Value("${app.recommendation-service.host}") String recommendationServiceHost,
        @Value("${app.recommendation-service.port}") int    recommendationServicePort,

        @Value("${app.review-service.host}") String reviewServiceHost,
        @Value("${app.review-service.port}") int    reviewServicePort
    ) {

        this.restTemplate = restTemplate;
        this.mapper = mapper;

        /** The body of the constructor builds the URLs based on the injected values */
        productServiceUrl        = "http://" + productServiceHost + ":" + productServicePort + "/product/";
        recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort + "/recommendation?productId=";
        reviewServiceUrl         = "http://" + reviewServiceHost + ":" + reviewServicePort + "/review?productId=";
    }

    /** Method to make the call to product-service and return a product */
    public Product getProduct(int productId) {

        try {
            String url = productServiceUrl + productId;
            LOG.debug("Will call getProduct API on URL: {}", url);

            Product product = restTemplate.getForObject(url, Product.class);
            LOG.debug("Found a product with id: {}", product.getProductId());

            return product;

        } catch (HttpClientErrorException ex) {

            switch (ex.getStatusCode()) {

            case NOT_FOUND:
                throw new NotFoundException(getErrorMessage(ex));

            case UNPROCESSABLE_ENTITY :
                throw new InvalidInputException(getErrorMessage(ex));

            default:
                LOG.warn("Got a unexpected HTTP error: {}, will rethrow it", ex.getStatusCode());
                LOG.warn("Error body: {}", ex.getResponseBodyAsString());
                throw ex;
            }
        }
    }

    private String getErrorMessage(HttpClientErrorException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }

    /** Method to make the call to recommendation-service and return a list of recommendations */
    public List<Recommendation> getRecommendations(int productId) {

        try {
            String url = recommendationServiceUrl + productId;

            LOG.debug("Will call getRecommendations API on URL: {}", url);
            List<Recommendation> recommendations = restTemplate.exchange(url, GET, null, new ParameterizedTypeReference<List<Recommendation>>() {}).getBody();

            LOG.debug("Found {} recommendations for a product with id: {}", recommendations.size(), productId);
            return recommendations;

        } catch (Exception ex) {
            LOG.warn("Got an exception while requesting recommendations, return zero recommendations: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }

    /** Method to make the call to review-service and return a list of reviews */
    public List<Review> getReviews(int productId) {

        try {
            String url = reviewServiceUrl + productId;

            LOG.debug("Will call getReviews API on URL: {}", url);
            List<Review> reviews = restTemplate.exchange(url, GET, null, new ParameterizedTypeReference<List<Review>>() {}).getBody();

            LOG.debug("Found {} reviews for a product with id: {}", reviews.size(), productId);
            return reviews;

        } catch (Exception ex) {
            LOG.warn("Got an exception while requesting reviews, return zero reviews: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }
}

/**************************************************** -NOTES- *********************************************************/

/**
 * For the getProduct() implementation, the getForObject() method can be used in RestTemplate. 
 * The expected response is a Product object, and it can be expressed in the call to getForObject()
 * by specifying the Product.class class that RestTemplate will map the JSON response to. 
 *
 *
 * The getRecommendations() and getReviews() methods expect a generic list in the responses,
 * that is, List<Recommendation> and List<Review>. Since generics don't hold any type of information at runtime,
 * we can't specify that the methods expect a generic list in their responses. Instead, we can use a helper class
 * from the Spring Framework, ParameterizedTypeReference, that is designed to resolve this problem by holding the
 * type information at runtime. This means that RestTemplate can figure out what class to map the JSON responses to.
 * To utilize this helper class, we have to use the more involved exchange() method instead of the simpler getForObject() 
 * method on RestTemplate.
 *
 * */
