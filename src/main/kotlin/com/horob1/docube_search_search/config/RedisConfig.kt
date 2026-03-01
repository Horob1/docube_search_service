package com.horob1.docube_search_search.config

import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class RedisConfig(private val redisProperties: RedisProperties) {

    @Bean
    fun redisConnectionFactory(): LettuceConnectionFactory {
        val cfg = RedisStandaloneConfiguration()
        cfg.hostName = redisProperties.host
        cfg.port = redisProperties.port
        cfg.database = redisProperties.database
        if (!redisProperties.password.isNullOrEmpty()) {
            cfg.setPassword(redisProperties.password)
        }

        // Configure Lettuce client with optional timeout from properties (fallback to 5s)
        val timeout = redisProperties.timeout ?: Duration.ofMillis(5000)
        val clientConfig: LettuceClientConfiguration = LettuceClientConfiguration.builder()
            .commandTimeout(timeout)
            .build()

        return LettuceConnectionFactory(cfg, clientConfig)
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = redisConnectionFactory()
        template.keySerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer()
        template.hashValueSerializer = GenericJackson2JsonRedisSerializer()
        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun cacheManager(): CacheManager {
        val jsonSerializer = RedisSerializationContext.SerializationPair
            .fromSerializer(GenericJackson2JsonRedisSerializer())

        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(jsonSerializer)
            .prefixCacheNameWith("search:")

        // Per-cache TTL overrides
        val cacheConfigs = mapOf(
            "documents" to defaultConfig.entryTtl(Duration.ofMinutes(15)),
            "authors" to defaultConfig.entryTtl(Duration.ofMinutes(30)),
            "trending" to defaultConfig.entryTtl(Duration.ofMinutes(5)),
            "popular" to defaultConfig.entryTtl(Duration.ofMinutes(10)),
            "suggest" to defaultConfig.entryTtl(Duration.ofMinutes(10))
        )

        return RedisCacheManager.builder(redisConnectionFactory())
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .transactionAware()
            .build()
    }
}
