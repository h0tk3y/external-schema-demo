
import com.h0tk3y.kotlin.staticObjectNotation.analysis.AnalysisSchema
import com.h0tk3y.kotlin.staticObjectNotation.analysis.ResolutionResult
import com.h0tk3y.kotlin.staticObjectNotation.analysis.SchemaTypeRefContext
import com.h0tk3y.kotlin.staticObjectNotation.analysis.defaultCodeResolver
import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.DefaultLanguageTreeBuilder
import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.LanguageTreeResult
import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.parseToLightTree
import com.h0tk3y.kotlin.staticObjectNotation.dom.resolvedDocument
import com.h0tk3y.kotlin.staticObjectNotation.dom.toDocument
import com.h0tk3y.kotlin.staticObjectNotation.language.SourceIdentifier
import com.h0tk3y.kotlin.staticObjectNotation.objectGraph.*

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

internal fun languageModelFromScriptSource(scriptSource: String): LanguageTreeResult {
    val (tree, code, codeOffset) = parseToLightTree(scriptSource)
    return DefaultLanguageTreeBuilder().build(tree, code, codeOffset, SourceIdentifier("script file"))
}

private fun assignmentTrace(result: ResolutionResult) =
    AssignmentTracer { AssignmentResolver() }.produceAssignmentTrace(result)

