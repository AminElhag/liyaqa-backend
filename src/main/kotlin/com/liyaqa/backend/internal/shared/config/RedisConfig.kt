package com.liyaqa.backend.internal.shared.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Redis configuration for distributed session storage and token blacklist.
 *
 * This configuration enables horizontal scaling of the backend by externalizing
 * session state to Redis. Key design decisions:
 *
 * 1. **Jackson JSON Serialization**: Sessions are stored as JSON for:
 *    - Human readability when debugging Redis
 *    - Cross-language compatibility if needed
 *    - Flexibility in schema evolution
 *
 * 2. **String Keys**: Simple string keys for efficient lookups:
 *    - `liyaqa:session:token:{refreshToken}` - Primary lookup by token
 *    - `liyaqa:session:employee:{employeeId}:{sessionId}` - For multi-session management
 *    - `liyaqa:blacklist:token:{tokenHash}` - JWT token blacklist
 *
 * 3. **TTL Management**: Redis automatically expires keys:
 *    - Sessions: 8-hour default (work day duration)
 *    - Blacklisted tokens: Match JWT expiration
 *
 * 4. **Lettuce Connection**: Async, high-performance Redis client with:
 *    - Connection pooling for concurrent requests
 *    - Automatic reconnection on network issues
 *    - Thread-safe operations
 *
 * Trade-offs:
 * - Network latency vs. in-memory speed (acceptable for session operations)
 * - JSON serialization overhead vs. readability (minimal impact)
 * - Redis dependency vs. stateless purity (necessary for immediate revocation)
 */
@Configuration
class RedisConfig {

    /**
     * Primary RedisTemplate for session and token storage.
     *
     * Configured with Jackson JSON serialization for both keys and values.
     * This template is thread-safe and can be injected anywhere.
     */
    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory

        // Use String serialization for keys (simple, efficient)
        val stringSerializer = StringRedisSerializer()
        template.keySerializer = stringSerializer
        template.hashKeySerializer = stringSerializer

        // Use Jackson JSON serialization for values (readable, flexible)
        val jsonSerializer = Jackson2JsonRedisSerializer(Any::class.java)

        // Configure ObjectMapper for Kotlin and Java 8 time types
        val objectMapper = ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(JavaTimeModule())
            // Don't fail on unknown properties (forward compatibility)
            configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

            // Define polymorphic type validator for secure deserialization
            val polymorphicTypeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Any::class.java)
                .build()

            // Include type information for polymorphic deserialization
            activateDefaultTyping(
                polymorphicTypeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL
            )
        }

        jsonSerializer.setObjectMapper(objectMapper)

        template.valueSerializer = jsonSerializer
        template.hashValueSerializer = jsonSerializer

        template.afterPropertiesSet()
        return template
    }
}
