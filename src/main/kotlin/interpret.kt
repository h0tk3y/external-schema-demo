
import com.h0tk3y.kotlin.staticObjectNotation.analysis.AnalysisSchema
import com.h0tk3y.kotlin.staticObjectNotation.analysis.ResolutionResult
import com.h0tk3y.kotlin.staticObjectNotation.analysis.SchemaTypeRefContext
import com.h0tk3y.kotlin.staticObjectNotation.analysis.defaultCodeResolver
import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.DefaultLanguageTreeBuilder
import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.LanguageTreeResult
import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.parseToLightTree
import com.h0tk3y.kotlin.staticObjectNotation.language.SourceIdentifier
import com.h0tk3y.kotlin.staticObjectNotation.objectGraph.*
import com.h0tk3y.kotlin.staticObjectNotation.serialization.SchemaSerialization
import java.io.File

internal fun interpretFromFiles(scriptFile: File, schemaFile: File) {
    val scriptContent = scriptFile.readText()
    val schema = SchemaSerialization.schemaFromJsonString(schemaFile.readText())
    
    val context = contextForSchemaFile(schemaFile)
    
    val topLevelObject = interpret(scriptContent, schema, context)
    println(prettyStringFromReflection(topLevelObject))
}

private fun interpret(
    scriptContent: String,
    analysisSchema: AnalysisSchema,
    evaluationContext: EvaluationContext
): ObjectReflection {
    val languageModel = languageModelFromScriptSource(scriptContent)
    
    val failures = languageModel.allFailures
    if (failures.isNotEmpty()) {
        println(failures)
        error("failures found in the script")
    }
    
    val resolver = defaultCodeResolver(analysisStatementFilterFor(evaluationContext))
    val resolution = resolver.resolve(analysisSchema, languageModel.imports, languageModel.topLevelBlock)
    if (resolution.errors.isNotEmpty()) {
        println(resolution.errors)
        error("errors in resolution")
    }

    val trace = assignmentTrace(resolution)
    val unassignedValueUsages = trace.elements.filterIsInstance<AssignmentTraceElement.UnassignedValueUsed>()
    if (unassignedValueUsages.isNotEmpty()) {
        error("errors in assignment resolution")
    }
    val context = ReflectionContext(SchemaTypeRefContext(analysisSchema), resolution, trace)

    return reflect(resolution.topLevelReceiver, context)
}

private fun languageModelFromScriptSource(scriptSource: String): LanguageTreeResult {
    val (tree, code, codeOffset) = parseToLightTree(scriptSource)
    return DefaultLanguageTreeBuilder().build(tree, code, codeOffset, SourceIdentifier("script file"))
}

private fun assignmentTrace(result: ResolutionResult) =
    AssignmentTracer { AssignmentResolver() }.produceAssignmentTrace(result)

