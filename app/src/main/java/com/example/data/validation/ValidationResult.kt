package com.example.data.validation

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val topicCount: Int = 0,
    val sectionCount: Int = 0
)

enum class ParsedContentType {
    TOPIC_PACK,
    TOPIC_PACK_ARRAY,
    FULL_BACKUP,
    UNKNOWN
}

data class ParsedPayload(
    val type: ParsedContentType,
    val validationResult: ValidationResult,
    val rawJson: String
)
