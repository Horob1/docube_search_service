package com.horob1.docube_search_search.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

@Configuration
@EnableElasticsearchRepositories(basePackages = ["com.horob1.docube_search_search.repository"])
class ElasticsearchConfig

