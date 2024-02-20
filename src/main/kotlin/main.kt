import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import org.gradle.internal.declarativedsl.serialization.SchemaSerialization

class ExternalSchemaDemo : CliktCommand() {
    private enum class InterpretationMode {
        LOWLEVEL, DOM
    }
    
    private val script by option().file(mustBeReadable = true).required()
    private val schema by option().file(mustBeReadable = true)
    private val mode by option().enum<InterpretationMode>().default(InterpretationMode.LOWLEVEL)
    private val locations by option().boolean().default(false)

    override fun run() {
        val scriptContent = script.readText()
        val languageModel = languageModelFromScriptSource(scriptContent)
        val analysisSchema = schema?.let { SchemaSerialization.schemaFromJsonString(it.readText()) }
        val evaluationContext = schema?.let { contextForSchemaFile(it) } ?: EvaluationContext.Unknown

        when (mode) {
            InterpretationMode.LOWLEVEL -> {
                requireNotNull(analysisSchema) { "analysis schema is required for $mode processing" }
                require(!locations) { "printing locations is not supported in $mode processing" }
                interpretWithObjectReflection(languageModel, analysisSchema, evaluationContext)
            }

            InterpretationMode.DOM ->
                interpretWithDom(languageModel, analysisSchema, evaluationContext, locations)
        }
    }
}

fun main(args: Array<String>) = ExternalSchemaDemo().main(args)
