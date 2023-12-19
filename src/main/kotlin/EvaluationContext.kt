import com.h0tk3y.kotlin.staticObjectNotation.analysis.AnalysisStatementFilter
import com.h0tk3y.kotlin.staticObjectNotation.analysis.ObjectOrigin
import com.h0tk3y.kotlin.staticObjectNotation.analysis.analyzeEverything
import com.h0tk3y.kotlin.staticObjectNotation.language.DataStatement
import com.h0tk3y.kotlin.staticObjectNotation.language.FunctionArgument
import com.h0tk3y.kotlin.staticObjectNotation.language.FunctionCall
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