package com.project.upbit_clone.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(modifyOnCreate = true)
public class JpaAuditingConfig {
}
