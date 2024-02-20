import org.gradle.internal.declarativedsl.analysis.ObjectOrigin
import org.gradle.internal.declarativedsl.language.DataType
import org.gradle.internal.declarativedsl.objectGraph.ObjectReflection

internal fun prettyStringFromReflection(objectReflection: ObjectReflection): String {
    val visitedIdentity = mutableSetOf<Long>()

    fun StringBuilder.recurse(current: ObjectReflection, depth: Int) {
        fun indent() = "    ".repeat(depth)
        fun nextIndent() = "    ".repeat(depth + 1)
        when (current) {
            is ObjectReflection.ConstantValue -> append(
                if (current.type == DataType.StringDataType)
                    "\"${current.value}\""
                else current.value.toString()
            )

            is ObjectReflection.DataObjectReflection -> {
                append(current.type.toString() + (if (current.identity != -1L) "#" + current.identity else "") + " ")
                if (current.identity in -1L..0L || visitedIdentity.add(current.identity)) {
                    if (shouldMentionOrigin(current.objectOrigin)) {
                        append("from " + current.objectOrigin + " ")
                    }
                    if (current.properties.isNotEmpty() || current.addedObjects.isNotEmpty()) {
                        append("{\n")
                        current.properties.forEach {
                            val valueReflection = it.value.value
                            if (valueReflection is ObjectReflection.DataObjectReflection &&
                                isDefaultOnlyObject(valueReflection)
                            ) {
                                return@forEach
                            }
                            append(nextIndent() + it.key.name + " = ")
                            recurse(valueReflection, depth + 1)
                            append("\n")
                        }
                        current.addedObjects.forEach {
                            append("${nextIndent()}+ added ")
                            recurse(it, depth + 1)
                            append("\n")
                        }
                        current.customAccessorObjects.forEach { 
                            append("${nextIndent()}.${customAccessorOriginToString(it.objectOrigin)} ")
                            recurse(it, depth + 1)
                            append("\n")
                        }
                        current.lambdaAccessedObjects.forEach { 
                            append("${nextIndent()}.${lambdaAccessorOriginToString(it.objectOrigin)} ")
                            recurse(it, depth + 1)
                            append("\n")
                        }
                        append("${indent()}}")
                    }
                } else {
                    append("{ ... }")
                }
            }

            is ObjectReflection.External -> append("(external ${current.key.type}})")
            is ObjectReflection.PureFunctionInvocation -> {
                append(current.objectOrigin.function.simpleName)
                append("#" + current.objectOrigin.invocationId)
                if (visitedIdentity.add(current.objectOrigin.invocationId)) {
                    append("(")
                    if (current.parameterResolution.isNotEmpty()) {
                        append("\n")
                        for (param in current.parameterResolution) {
                            append("${nextIndent()}${param.key.name}")
                            append(" = ")
                            recurse(param.value, depth + 1)
                            append("\n")
                        }
                        append(indent())
                    }
                    append(")")
                } else {
                    append("(...)")
                }
            }

            is ObjectReflection.DefaultValue -> append("(default value)")
            is ObjectReflection.AddedByUnitInvocation -> append("by call: ${current.objectOrigin}")
            is ObjectReflection.Null -> append("null")
        }
    }

    return buildString { recurse(objectReflection, 0) }
}

private fun customAccessorOriginToString(objectOrigin: ObjectOrigin) = when (objectOrigin) {
    is ObjectOrigin.CustomConfigureAccessor -> objectOrigin.accessor.customAccessorIdentifier
    else -> objectOrigin.toString()
}

private fun lambdaAccessorOriginToString(objectOrigin: ObjectOrigin) = when (objectOrigin) {
    is ObjectOrigin.ConfiguringLambdaReceiver -> objectOrigin.function.simpleName + "{}"
    else -> objectOrigin.toString()
}

private fun isDefaultOnlyObject(obj: ObjectReflection.DataObjectReflection): Boolean {
    fun isDefault(o: ObjectReflection) = when (o) {
        is ObjectReflection.DataObjectReflection -> isDefaultOnlyObject(o)
        is ObjectReflection.DefaultValue -> true
        else -> false
    }

    return obj.properties.values.all { isDefault(it.value) } && obj.addedObjects.isEmpty()
}

private fun shouldMentionOrigin(objectOrigin: ObjectOrigin) = when (objectOrigin) {
    is ObjectOrigin.TopLevelReceiver,
    is ObjectOrigin.PropertyDefaultValue,
    is ObjectOrigin.PropertyReference -> false

    else -> true
}
