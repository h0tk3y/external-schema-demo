import org.gradle.internal.declarativedsl.analysis.*
import org.gradle.internal.declarativedsl.analysis.AnalysisStatementFilter.Companion.isCallNamed
import org.gradle.internal.declarativedsl.analysis.AnalysisStatementFilter.Companion.isConfiguringCall
import org.gradle.internal.declarativedsl.analysis.AnalysisStatementFilter.Companion.isTopLevelElement
import java.io.File
import java.util.Locale

enum class EvaluationContext {
    SettingsPluginManagement,
    SettingsPlugins,
    Settings,
    Project,
    Unknown {
        override val fileName get() = null
    };
    
    open val fileName: String? = name.replaceFirstChar { it.lowercase(Locale.getDefault()) }
}

internal fun contextForSchemaFile(schemaFile: File) =
    enumValues<EvaluationContext>().find { schemaFile.name == it.fileName + schemaFilenameSuffix }


private const val schemaFilenameSuffix = ".dcl.schema"

internal fun analysisStatementFilterFor(evaluationContext: EvaluationContext) = when (evaluationContext) {
    EvaluationContext.Project -> analyzeEverything
    EvaluationContext.Settings -> ignorePluginsAndPluginManagement
    EvaluationContext.SettingsPluginManagement -> pluginManagementOnly
    EvaluationContext.SettingsPlugins -> pluginsOnly
    EvaluationContext.Unknown -> analyzeEverything
}

private val isPluginManagement = isCallNamed("pluginManagement").and(isConfiguringCall)
private val isPlugins = isCallNamed("plugins").and(isConfiguringCall)
private val pluginsOnly = isTopLevelElement.implies(isPlugins)
private val pluginManagementOnly = isTopLevelElement.implies(isPluginManagement)
private val ignorePluginsAndPluginManagement = isTopLevelElement.implies(isPlugins.not().and(isPluginManagement.not()))
