import com.h0tk3y.kotlin.staticObjectNotation.analysis.*
import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.*
import com.h0tk3y.kotlin.staticObjectNotation.language.AstSourceIdentifier
import com.h0tk3y.kotlin.staticObjectNotation.language.DataStatement
import com.h0tk3y.kotlin.staticObjectNotation.language.FunctionArgument
import com.h0tk3y.kotlin.staticObjectNotation.language.FunctionCall
import com.h0tk3y.kotlin.staticObjectNotation.objectGraph.*
import com.h0tk3y.kotlin.staticObjectNotation.serialization.SchemaSerialization
import kotlinx.ast.common.ast.Ast
import org.antlr.v4.kotlinruntime.misc.ParseCancellationException
import java.io.File
import java.util.Collections.emptyList

internal fun interpretFromFiles(scriptFile: File, schemaFile: File) {
    val scriptContent = scriptFile.readText()
    val schema = SchemaSerialization.schemaFromJsonString(schemaFile.readText())
    
    val topLevelObject = interpret(scriptContent, schema)
    println(prettyStringFromReflection(topLevelObject))
}

private fun interpret(
    scriptContent: String,
    analysisSchema: AnalysisSchema
): ObjectReflection {
    val resolver = defaultCodeResolver(analyzeEverythingExceptPluginsBlock)
    val ast = astFromScript(scriptContent).singleOrNull()
        ?: error("no AST produced from script source")

    val languageModel = languageModelFromAst(ast)
    val failures = languageModel.results.filterIsInstance<FailingResult>()
    if (failures.isNotEmpty()) {
        println(failures)
        error("failures found in the script")
    }
    val elements = languageModel.results.filterIsInstance<Element<*>>().map { it.element }
    val resolution = resolver.resolve(analysisSchema, elements)
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

private
val analyzeEverythingExceptPluginsBlock = AnalysisStatementFilter { statement, scopes ->
    if (scopes.last().receiver is ObjectOrigin.TopLevelReceiver) {
        !isPluginsCall(statement)
    } else true
}

private
fun isPluginsCall(statement: DataStatement) =
    statement is FunctionCall && statement.name == "plugins" && statement.args.size == 1 && statement.args.single() is FunctionArgument.Lambda

private
fun languageModelFromAst(ast: Ast): LanguageTreeResult =
    languageTreeBuilder.build(ast, AstSourceIdentifier(ast, "source"))

private
val languageTreeBuilder = LanguageTreeBuilderWithTopLevelBlock(DefaultLanguageTreeBuilder())


private
fun astFromScript(scriptSource: String): List<Ast> =
    try {
        parseToAst(scriptSource)
    } catch (e: ParseCancellationException) {
        emptyList()
    }

private
fun assignmentTrace(result: ResolutionResult) =
    AssignmentTracer { AssignmentResolver() }.produceAssignmentTrace(result)
