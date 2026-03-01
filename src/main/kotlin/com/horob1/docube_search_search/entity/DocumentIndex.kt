package com.horob1.docube_search_search.entity

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.*

@Document(indexName = "documents")
@Setting(settingPath = "elasticsearch/documents-settings.json")
data class DocumentIndex(
    @Id
    val id: String,

    @MultiField(
        mainField = Field(type = FieldType.Text, analyzer = "autocomplete_analyzer", searchAnalyzer = "standard"),
        otherFields = [InnerField(suffix = "keyword", type = FieldType.Keyword)]
    )
    val title: String? = null,

    @Field(type = FieldType.Text)
    val description: String? = null,

    @Field(type = FieldType.Text)
    val content: String? = null,

    @Field(type = FieldType.Keyword)
    val tags: List<String>? = null,

    @Field(type = FieldType.Keyword)
    val categories: List<String>? = null,

    @Field(type = FieldType.Keyword)
    val language: String? = null,

    @Field(type = FieldType.Keyword)
    val status: String? = null,

    @Field(type = FieldType.Keyword)
    val visibility: String? = null,

    @Field(type = FieldType.Date, format = [DateFormat.date_optional_time, DateFormat.epoch_millis])
    val createdAt: String? = null,

    @Field(type = FieldType.Date, format = [DateFormat.date_optional_time, DateFormat.epoch_millis])
    val updatedAt: String? = null,

    @Field(type = FieldType.Date, format = [DateFormat.date_optional_time, DateFormat.epoch_millis])
    val publishedAt: String? = null,

    @Field(type = FieldType.Long)
    val viewCount: Long? = 0,

    @Field(type = FieldType.Long)
    val likeCount: Long? = 0,

    @Field(type = FieldType.Long)
    val commentCount: Long? = 0,

    @Field(type = FieldType.Float)
    val score: Float? = 0f,

    @Field(type = FieldType.Object)
    val author: EmbeddedAuthor? = null
)

data class EmbeddedAuthor(
    @Field(type = FieldType.Keyword)
    val id: String? = null,

    @Field(type = FieldType.Text)
    val username: String? = null,

    @Field(type = FieldType.Text, analyzer = "autocomplete_analyzer", searchAnalyzer = "standard")
    val displayName: String? = null,

    @Field(type = FieldType.Keyword)
    val email: String? = null,

    @Field(type = FieldType.Keyword)
    val avatarUrl: String? = null,

    @Field(type = FieldType.Keyword)
    val role: String? = null
)

