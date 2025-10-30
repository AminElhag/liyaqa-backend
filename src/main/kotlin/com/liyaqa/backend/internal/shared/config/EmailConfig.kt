package com.liyaqa.backend.internal.shared.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.*

/**
 * Email configuration for transactional notifications.
 *
 * This configuration sets up JavaMailSender with SMTP settings for sending
 * internal team notifications. In production, this would connect to services
 * like SendGrid, AWS SES, or a corporate SMTP server.
 *
 * Design considerations:
 * - Externalized configuration for easy environment-specific setup
 * - Support for TLS/SSL encryption
 * - Connection pooling and timeout management
 * - Graceful degradation when email is disabled
 */
@Configuration
class EmailConfig {

    @Bean
    fun javaMailSender(
        @Value("\${spring.mail.host:smtp.gmail.com}") host: String,
        @Value("\${spring.mail.port:587}") port: Int,
        @Value("\${spring.mail.username:}") username: String,
        @Value("\${spring.mail.password:}") password: String,
        @Value("\${spring.mail.protocol:smtp}") protocol: String,
        @Value("\${spring.mail.properties.mail.smtp.auth:true}") smtpAuth: Boolean,
        @Value("\${spring.mail.properties.mail.smtp.starttls.enable:true}") starttlsEnable: Boolean,
        @Value("\${spring.mail.properties.mail.smtp.starttls.required:false}") starttlsRequired: Boolean,
        @Value("\${spring.mail.properties.mail.smtp.connectiontimeout:5000}") connectionTimeout: Int,
        @Value("\${spring.mail.properties.mail.smtp.timeout:5000}") timeout: Int,
        @Value("\${spring.mail.properties.mail.smtp.writetimeout:5000}") writeTimeout: Int
    ): JavaMailSender {
        val mailSender = JavaMailSenderImpl()

        // Basic configuration
        mailSender.host = host
        mailSender.port = port
        mailSender.username = username
        mailSender.password = password
        mailSender.protocol = protocol

        // Additional properties for security and performance
        val props: Properties = mailSender.javaMailProperties
        props["mail.smtp.auth"] = smtpAuth
        props["mail.smtp.starttls.enable"] = starttlsEnable
        props["mail.smtp.starttls.required"] = starttlsRequired
        props["mail.smtp.connectiontimeout"] = connectionTimeout
        props["mail.smtp.timeout"] = timeout
        props["mail.smtp.writetimeout"] = writeTimeout

        // Enable debugging in development (set via properties)
        props["mail.debug"] = System.getProperty("mail.debug", "false")

        return mailSender
    }
}
