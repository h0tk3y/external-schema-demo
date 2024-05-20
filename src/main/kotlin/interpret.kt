
import org.gradle.declarative.dsl.schema.AnalysisSchema
import org.gradle.internal.declarativedsl.analysis.ResolutionResult
import org.gradle.internal.declarativedsl.analysis.SchemaTypeRefContext
import org.gradle.internal.declarativedsl.analysis.defaultCodeResolver
import org.gradle.internal.declarativedsl.dom.resolvedDocument
import org.gradle.internal.declarativedsl.dom.toDocument
import org.gradle.internal.declarativedsl.language.SourceIdentifier
import org.gradle.internal.declarativedsl.objectGraph.*
import org.gradle.internal.declarativedsl.parsing.DefaultLanguageTreeBuilder
import org.gradle.internal.declarativedsl.language.LanguageTreeResult
import org.gradle.internal.declarativedsl.parsing.parse

internal fun interpretWithObjectReflection(
    languageModel: LanguageTreeResult,
    schema: AnalysisSchema,
    evaluationContext: EvaluationContext
) {
    val topLevelObject = interpret(languageModel, schema, evaluationContext)
    println(prettyStringFromReflection(topLevelObject))
}

internal fun interpretWithDom(
    languageModel: LanguageTreeResult,
    schema: AnalysisSchema?,
    evaluationContext: EvaluationContext,
    sourceLocations: Boolean
) {
    val document =
        if (schema != null) 
            resolvedDocument(schema, languageModel, analysisStatementFilterFor(evaluationContext))
        else languageModel.toDocument()
    
    println(prettyStringFromDom(document, sourceLocations, schema))
}

private fun interpret(
    languageModel: LanguageTreeResult,
    analysisSchema: AnalysisSchema,
    evaluationContext: EvaluationContext
): ObjectReflection {
    
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

internal fun languageModelFromScriptSource(scriptSource: String): LanguageTreeResult =
    DefaultLanguageTreeBuilder().build(parse(scriptSource), SourceIdentifier("script file"))

private fun assignmentTrace(result: ResolutionResult) =
    AssignmentTracer { AssignmentResolver() }.produceAssignmentTrace(result)

