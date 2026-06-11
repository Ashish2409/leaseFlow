package com.abm.leaseFlow.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

// @EnableJpaAuditing is declared on LeaseFlowApplication — do not duplicate it here.
@Configuration
@EnableJpaRepositories(basePackages = "com.abm.leaseFlow")
@EnableTransactionManagement
public class JpaConfig {
}
