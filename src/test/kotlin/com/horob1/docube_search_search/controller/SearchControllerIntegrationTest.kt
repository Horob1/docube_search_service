package com.horob1.docube_search_search.controller

import com.horob1.docube_search_search.dto.PageResponse
import com.horob1.docube_search_search.dto.request.DocumentSearchRequest
import com.horob1.docube_search_search.dto.response.DocumentSearchResponse
import com.horob1.docube_search_search.service.AuthorSearchService
import com.horob1.docube_search_search.service.DocumentSearchService
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SearchController::class)
class SearchControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var documentSearchService: DocumentSearchService

    @MockitoBean
    private lateinit var authorSearchService: AuthorSearchService

    @Test
    fun `search documents should map school and faculty filters into request`() {
        val expectedRequest = DocumentSearchRequest(
            keyword = "Công nghệ thông tin",
            page = 0,
            size = 10,
            sortBy = "relevance",
            order = "desc",
            status = null,
            authorId = null,
            schoolId = "HCMUS",
            facultyId = "CNTT"
        )

        val response = PageResponse(
            content = listOf(
                DocumentSearchResponse(
                    id = "doc-1",
                    title = "Giới thiệu Công nghệ thông tin",
                    description = "Tài liệu định hướng",
                    content = "Nội dung",
                    schoolId = "HCMUS",
                    schoolName = "Trường Đại học Khoa học Tự nhiên",
                    facultyId = "CNTT",
                    facultyName = "Công nghệ thông tin",
                    tags = listOf("cntt"),
                    categories = listOf("Học tập"),
                    language = "vi",
                    status = "PUBLISHED",
                    visibility = "PUBLIC",
                    createdAt = null,
                    updatedAt = null,
                    publishedAt = null,
                    viewCount = 10,
                    likeCount = 1,
                    commentCount = 0,
                    score = 4.0f,
                    author = null,
                    highlights = null
                )
            ),
            page = 0,
            size = 10,
            totalElements = 1,
            totalPages = 1,
            hasNext = false
        )

        Mockito.doReturn(response)
            .`when`(documentSearchService)
            .search(expectedRequest)

        mockMvc.perform(
            get("/api/v1/search/documents")
                .param("keyword", "Công nghệ thông tin")
                .param("school_id", "HCMUS")
                .param("faculty_id", "CNTT")
                .param("page", "0")
                .param("size", "10")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].schoolId").value("HCMUS"))
            .andExpect(jsonPath("$.content[0].facultyId").value("CNTT"))

        Mockito.verify(documentSearchService).search(expectedRequest)
    }

    @Test
    fun `search documents should keep school and faculty null when params are missing`() {
        val expectedRequest = DocumentSearchRequest(
            keyword = "Elasticsearch",
            page = 0,
            size = 10,
            sortBy = "relevance",
            order = "desc",
            status = null,
            authorId = null,
            schoolId = null,
            facultyId = null
        )

        val emptyResponse = PageResponse<DocumentSearchResponse>(
            content = emptyList(),
            page = 0,
            size = 10,
            totalElements = 0,
            totalPages = 0,
            hasNext = false
        )

        Mockito.doReturn(emptyResponse)
            .`when`(documentSearchService)
            .search(expectedRequest)

        mockMvc.perform(
            get("/api/v1/search/documents")
                .param("keyword", "Elasticsearch")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)

        Mockito.verify(documentSearchService).search(expectedRequest)
        assertNull(expectedRequest.schoolId)
        assertNull(expectedRequest.facultyId)
    }
}
