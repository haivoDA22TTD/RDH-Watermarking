package com.rdh.backend.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * Configuration for Bean Validation
 * Sets up message source, validator factory, and method validation
 */
@Configuration
public class ValidationConfig {
    
    /**
     * Configure MessageSource for validation messages
     * Loads messages from ValidationMessages.properties
     */
    @Bean
    public MessageSource validationMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("ValidationMessages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600); // Cache for 1 hour
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
    
    /**
     * Configure LocalValidatorFactoryBean
     * This is the main validator that uses our custom message source
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
        validatorFactoryBean.setValidationMessageSource(validationMessageSource());
        return validatorFactoryBean;
    }
    
    /**
     * Enable method-level validation for @Validated on classes
     * This allows validation of @RequestParam, @PathVariable, etc.
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator());
        return processor;
    }
}
