package at.gv.brz.common.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.moshi.JsonClass

/**
 * Model object containing a JSONlogic condition as string
 */
@JsonClass(generateAdapter = true)
data class CertificateConditionModel(val logic: String) {

    fun parsedLogic(objectMapper: ObjectMapper): JsonNode? {
        try {
            return objectMapper.readValue(logic, JsonNode::class.java)
        } catch (ignored: Exception) {
        }
        return null
    }
}