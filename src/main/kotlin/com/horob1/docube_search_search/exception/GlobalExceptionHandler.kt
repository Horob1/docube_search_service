package com.horob1.docube_search_search.exception

import com.horob1.docube_search_search.dto.response.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.apache.kafka.common.KafkaException
import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                status = 404,
                error = "Not Found",
                message = ex.message,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(SearchException::class)
    fun handleSearchException(ex: SearchException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.error("Search error: {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                status = 500,
                error = "Search Error",
                message = ex.message,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(UncategorizedElasticsearchException::class)
    fun handleElasticsearch(ex: UncategorizedElasticsearchException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.error("Elasticsearch error: {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            ErrorResponse(
                status = 503,
                error = "Elasticsearch Unavailable",
                message = "Elasticsearch cluster is not available",
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(KafkaException::class)
    fun handleKafka(ex: KafkaException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.error("Kafka error: {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            ErrorResponse(
                status = 503,
                error = "Kafka Error",
                message = "Kafka broker communication error",
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                status = 400,
                error = "Validation Error",
                message = errors,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(ex: MissingServletRequestParameterException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                status = 400,
                error = "Missing Parameter",
                message = "Required parameter '${ex.parameterName}' is missing",
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                status = 400,
                error = "Bad Request",
                message = ex.message,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error: {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                status = 500,
                error = "Internal Server Error",
                message = ex.message,
                path = request.requestURI
            )
        )
    }
}

