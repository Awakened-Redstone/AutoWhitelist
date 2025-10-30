package com.awakenedredstone.multiversion

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode

inline fun <reified T : JsonNode> ObjectNode.putIfAbsentAndGet(propertyName: String, value: (JsonNodeFactory) -> T): T {
    val property = this.get(propertyName)
    if (property != null) {
        if (property !is T) throw IllegalStateException("Property $propertyName is not of type ${T::class.simpleName}")

        return property
    }

    val newValue = value(JsonNodeFactory.instance)
    this.set<T>(propertyName, newValue)

    return newValue
}