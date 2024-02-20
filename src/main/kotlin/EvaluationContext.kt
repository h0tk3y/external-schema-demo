import org.gradle.internal.declarativedsl.analysis.AnalysisStatementFilter
import org.gradle.internal.declarativedsl.analysis.ObjectOrigin
import org.gradle.internal.declarativedsl.analysis.analyzeEverything
import org.gradle.internal.declarativedsl.language.DataStatement
import org.gradle.internal.declarativedsl.language.FunctionArgument
import org.gradle.internal.declarativedsl.language.FunctionCall
import java.io.File

internal sealed interface EvaluationContext {
    val fileName: String?
    
    data object Settings : EvaluationContext {
        override val fileName = "settings"
    }
    
    data object Plugins : EvaluationContext {
        override val fileName = "plugins"
    }

    data object Project : EvaluationContext {
        override val fileName = "project"
    }
    
    data object Unknown : EvaluationContext {
        override val fileName = null
    }
}

internal fun contextForSchemaFile(schemaFile: File) =
    when (schemaFile.name) {
        "settings$schemaFilenameSuffix" -> EvaluationContext.Settings 
        "plugins$schemaFilenameSuffix" -> EvaluationContext.Plugins 
        "project$schemaFilenameSuffix" -> EvaluationContext.Project 
        else -> EvaluationContext.Unknown
    }

internal val schemaFilenameSuffix = ".something.schema"

internal fun analysisStatementFilterFor(evaluationContext: EvaluationContext) = when (evaluationContext) {
    EvaluationContext.Plugins -> analyzeTopLevelPluginsBlockOnly
    EvaluationContext.Project -> analyzeEverythingExceptPluginsBlock
    EvaluationContext.Settings -> analyzeEverythingExceptPluginsBlock
    EvaluationContext.Unknown -> analyzeEverything
}

private val analyzeTopLevelPluginsBlockOnly = AnalysisStatementFilter { statement, scopes ->
    if (scopes.last().receiver is ObjectOrigin.TopLevelReceiver) {
        isPluginsCall(statement)
    } else true
}

private val analyzeEverythingExceptPluginsBlock = AnalysisStatementFilter { statement, scopes ->
    if (scopes.last().receiver is ObjectOrigin.TopLevelReceiver) {
        !isPluginsCall(statement)
    } else true
}

private fun isPluginsCall(statement: DataStatement) =
    statement is FunctionCall && statement.name == "plugins" && statement.args.size == 1 && statement.args.single() is FunctionArgument.Lambda