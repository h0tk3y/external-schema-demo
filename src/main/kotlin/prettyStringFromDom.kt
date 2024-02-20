import org.gradle.internal.declarativedsl.analysis.SchemaTypeRefContext
import org.gradle.internal.declarativedsl.analysis.TypeRefContext
import org.gradle.internal.declarativedsl.dom.*
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.DocumentNode.*
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.ValueNode
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.ValueNode.LiteralValueNode
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument.ValueNode.ValueFactoryNode
import org.gradle.internal.declarativedsl.dom.DocumentResolution.ElementResolution.SuccessfulElementResolution
import org.gradle.internal.declarativedsl.dom.DocumentResolution.ValueResolution.ValueFactoryResolution
import org.gradle.internal.declarativedsl.dom.ResolvedDeclarativeDocument.ResolvedDocumentNode.ResolvedElementNode
import org.gradle.internal.declarativedsl.dom.ResolvedDeclarativeDocument.ResolvedDocumentNode.ResolvedPropertyNode
import org.gradle.internal.declarativedsl.dom.ResolvedDeclarativeDocument.ResolvedValueNode.ResolvedValueFactoryNode
import org.gradle.internal.declarativedsl.language.SourceData
import org.gradle.internal.declarativedsl.analysis.AnalysisSchema

internal fun prettyStringFromDom(
    document: DeclarativeDocument,
    withSourceLocations: Boolean,
    schema: AnalysisSchema?
): String {
    val settings = FormattingSettings(withSourceLocations)

    val maybeTypeRefContext by lazy {
        SchemaTypeRefContext(requireNotNull(schema) { "schema must be provided to handle a resolved document" })
    }
    
    val literalFormatter = LiteralFormatter(settings)

    val valueFormatter = ValueFormatter(
        when (document) {
            is ResolvedDeclarativeDocument -> ResolvedValueFactoryFormatter(settings, literalFormatter, maybeTypeRefContext)
            else -> RawValueFactoryFormatter(settings, literalFormatter)
        },
        literalFormatter
    )

    val propFormatter = when (document) {
        is ResolvedDeclarativeDocument -> ResolvedPropertyFormatter(settings, valueFormatter, maybeTypeRefContext)
        else -> RawPropertyFormatter(settings, valueFormatter)
    }

    val elementFormatter = when (document) {
        is ResolvedDeclarativeDocument -> ResolvedElementHeaderFormatter(settings, valueFormatter)
        else -> RawElementHeaderFormatter(settings, valueFormatter)
    }

    return buildString {
        fun visit(node: DeclarativeDocument.DocumentNode, depth: Int = 0) {
            val indent = "    ".repeat(depth)
            append(indent)
            when (node) {
                is ElementNode -> {
                    append(elementFormatter.formatElementHeader(node))
                    if (node.content.isNotEmpty()) {
                        appendLine(" {") 
                        node.content.forEach { visit(it, depth + 1) }
                        appendLine("$indent}")
                    } else {
                        appendLine()
                    }
                }

                is PropertyNode -> {
                    appendLine(propFormatter.formatPropertyLhs(node))
                }

                is ErrorNode -> appendLine(errorNode(node))
            }
        }

        document.content.forEach(::visit)
    }
}

class LiteralFormatter(private val settings: FormattingSettings) {
    fun formatLiteral(node: LiteralValueNode) =
        "literal${settings.sourceOrEmpty(node.sourceData)}(${formatValue(node.value)})"
    
    private fun formatValue(value: Any) = when (value) {
        is String -> "\"$value\""
        else -> value.toString()
    }
}

class ValueFormatter(
    private val valueFactoryFormatter: ValueFactoryFormatter,
    private val literalFormatter: LiteralFormatter
) {
    fun formatValue(value: ValueNode): String =
        when (value) {
            is LiteralValueNode -> literalFormatter.formatLiteral(value)
            is ValueFactoryNode -> valueFactoryFormatter.formatValueFactory(value)
        }
}

interface ValueFactoryFormatter {
    fun formatValueFactory(value: ValueFactoryNode): String
}

interface PropertyNodeFormatter {
    fun formatPropertyLhs(property: PropertyNode): String
}

interface ElementHeaderFormatter {
    fun formatElementHeader(element: ElementNode): String
}

data class FormattingSettings(
    val withSourceLocations: Boolean
)

class RawValueFactoryFormatter(private val settings: FormattingSettings, literalFormatter: LiteralFormatter) : ValueFactoryFormatter {
    private val valueFormatter = ValueFormatter(this, literalFormatter)
    
    override fun formatValueFactory(value: ValueFactoryNode): String =
        "valueFactory${settings.sourceOrEmpty(value.sourceData)}(" +
                (listOf("\"${value.factoryName}\"") + value.values.map { valueFormatter.formatValue(it) }).joinToString() +
                ")"
}

class RawPropertyFormatter(
    private val settings: FormattingSettings,
    private val valueFormatter: ValueFormatter
) : PropertyNodeFormatter {
    override fun formatPropertyLhs(property: PropertyNode): String =
        "property${settings.sourceOrEmpty(property.sourceData)}(" +
                "\"${property.name}\", " +
                valueFormatter.formatValue(property.value) +
                ")"
}

class RawElementHeaderFormatter(
    private val settings: FormattingSettings,
    private val valueFormatter: ValueFormatter
) : ElementHeaderFormatter {
    override fun formatElementHeader(element: ElementNode): String =
        "element${settings.sourceOrEmpty(element.sourceData)}(" +
                (listOf("\"${element.name}\"") + element.elementValues.map(valueFormatter::formatValue)).joinToString() +
                ")"
}

class ResolvedValueFactoryFormatter(
    private val settings: FormattingSettings,
    literalFormatter: LiteralFormatter,
    private val typeRefContext: TypeRefContext
) : ValueFactoryFormatter {
    private val valueFormatter = ValueFormatter(this, literalFormatter)
    
    override fun formatValueFactory(value: ValueFactoryNode): String {
        require(value is ResolvedValueFactoryNode)
        return "valueFactory${settings.sourceOrEmpty(value.sourceData)}(\"${value.factoryName}\", " +
                (listOf(resolutionString(value.resolution)) + value.values.map { valueFormatter.formatValue(it) }).joinToString() +
                ")"
    }

    private fun resolutionString(resolution: ValueFactoryResolution) = when (resolution) {
        is ValueFactoryResolution.ValueFactoryResolved -> "✓:${typeRefContext.resolveRef(resolution.function.returnValueType)}"
        is DocumentResolution.UnsuccessfulResolution -> errorReasons(resolution)
    }

}

class ResolvedPropertyFormatter(
    private val settings: FormattingSettings,
    private val valueFormatter: ValueFormatter,
    private val typeRefContext: TypeRefContext
) : PropertyNodeFormatter {
    override fun formatPropertyLhs(property: PropertyNode): String {
        require(property is ResolvedPropertyNode)
        return "property${settings.sourceOrEmpty(property.sourceData)}(" +
                "\"${property.name}\", " +
                "${resolutionString(property.resolution)}, " +
                valueFormatter.formatValue(property.value) +
                ")"
    }

    private fun resolutionString(resolution: DocumentResolution.PropertyResolution) = when (resolution) {
        is DocumentResolution.PropertyResolution.PropertyAssignmentResolved -> "✓:${typeRefContext.resolveRef(resolution.property.type)}"
        is DocumentResolution.UnsuccessfulResolution -> errorReasons(resolution)
    }
}

class ResolvedElementHeaderFormatter(
    private val settings: FormattingSettings,
    private val valueFormatter: ValueFormatter
) : ElementHeaderFormatter {
    override fun formatElementHeader(element: ElementNode): String {
        require(element is ResolvedElementNode)
        return "element${settings.sourceOrEmpty(element.sourceData)}(" +
                (listOf("\"${element.name}\"", resolutionString(element.resolution))
                        + element.elementValues.map(valueFormatter::formatValue)).joinToString() +
                ")"
    }

    private fun resolutionString(resolution: DocumentResolution.ElementResolution) = when (resolution) {
        is SuccessfulElementResolution -> "${resolutionPrefix(resolution)}:${resolution.elementType}"
        is DocumentResolution.UnsuccessfulResolution -> errorReasons(resolution)
    }

    private fun resolutionPrefix(resolution: SuccessfulElementResolution) = when (resolution) {
        is SuccessfulElementResolution.ContainerElementResolved -> "+"
        is SuccessfulElementResolution.PropertyConfiguringElementResolved -> "configure"
    }
}


private fun FormattingSettings.sourceOrEmpty(sourceData: SourceData) =
    if (!withSourceLocations) "" else "@${sourceData.indexRange} "

private fun errorReasons(resolution: DocumentResolution.UnsuccessfulResolution) =
    "⛌(${resolution.reasons.joinToString()})"

private fun errorNode(errorNode: ErrorNode) =
    "error(" + errorNode.errors.map { error ->
        when (error) {
            is SyntaxError -> error.parsingError.message
            is UnsupportedKotlinFeature -> "unsupported language feature ${error.unsupportedConstruct.languageFeature}"
            is UnsupportedSyntax -> "unsupported syntax ${error.cause}"
        }
    } + ")"
