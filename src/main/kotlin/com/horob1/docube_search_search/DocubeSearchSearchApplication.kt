package com.horob1.docube_search_search

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication
@EnableDiscoveryClient

class DocubeSearchSearchApplication

fun main(args: Array<String>) {
    runApplication<DocubeSearchSearchApplication>(*args)
}
