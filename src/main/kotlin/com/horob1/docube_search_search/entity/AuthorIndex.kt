package com.horob1.docube_search_search.entity

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.*

@Document(indexName = "authors")
@Setting(settingPath = "elasticsearch/authors-settings.json")
data class AuthorIndex(
    @Id
    val id: String,

    @MultiField(
        mainField = Field(type = FieldType.Text, analyzer = "autocomplete_analyzer", searchAnalyzer = "standard"),
        otherFields = [InnerField(suffix = "keyword", type = FieldType.Keyword)]
    )
    val username: String? = null,

    @MultiField(
        mainField = Field(type = FieldType.Text, analyzer = "autocomplete_analyzer", searchAnalyzer = "standard"),
        otherFields = [InnerField(suffix = "keyword", type = FieldType.Keyword)]
    )
    val displayName: String? = null,

    @Field(type = FieldType.Keyword)
    val email: String? = null,

    @Field(type = FieldType.Keyword)
    val phoneNumber: String? = null,

    @Field(type = FieldType.Keyword)
    val avatarUrl: String? = null,

    @Field(type = FieldType.Text)
    val bio: String? = null,

    @Field(type = FieldType.Keyword)
    val role: String? = null,

    @Field(type = FieldType.Keyword)
    val status: String? = null,

    @Field(type = FieldType.Date, format = [DateFormat.date_optional_time, DateFormat.epoch_millis])
    val createdAt: String? = null,

    @Field(type = FieldType.Long)
    val documentCount: Long? = 0,

    @Field(type = FieldType.Long)
    val followerCount: Long? = 0
)

