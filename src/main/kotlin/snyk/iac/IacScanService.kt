package snyk.iac

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import io.snyk.plugin.cli.CliError
import io.snyk.plugin.cli.ConsoleCommandRunner
import io.snyk.plugin.services.CliAdapter
import snyk.common.SnykError
import java.lang.reflect.Type

/**
 * Wrap work with Snyk CLI for IaC (`iac test` command).
 */
@Service
class IacScanService(project: Project) : CliAdapter<IacResult>(project) {

    fun scan(): IacResult = execute(listOf("iac", "test"))

    override fun getErrorResult(errorMsg: String): IacResult = IacResult(null, SnykError(errorMsg, projectPath))

    override fun convertRawCliStringToCliResult(rawStr: String): IacResult =
        try {
            val iacIssuesForFileListType: Type = object : TypeToken<ArrayList<IacIssuesForFile>>() {}.type
            when {
                rawStr == ConsoleCommandRunner.PROCESS_CANCELLED_BY_USER -> {
                    IacResult(null, null)
                }
                rawStr.isEmpty() -> {
                    IacResult(null, SnykError("CLI fail to produce any output", projectPath))
                }
                rawStr.first() == '[' -> {
                    IacResult(Gson().fromJson(rawStr, iacIssuesForFileListType), null)
                }
                rawStr.first() == '{' -> {
                    if (isSuccessCliJsonString(rawStr)) {
                        IacResult(listOf(Gson().fromJson(rawStr, IacIssuesForFile::class.java)), null)
                    } else {
                        val cliError = Gson().fromJson(rawStr, CliError::class.java)
                        IacResult(null, SnykError(cliError.message, cliError.path))
                    }
                }
                else -> {
                    IacResult(null, SnykError(rawStr, projectPath))
                }
            }
        } catch (e: JsonSyntaxException) {
            IacResult(null, SnykError(e.message ?: e.toString(), projectPath))
        }

    private fun isSuccessCliJsonString(jsonStr: String): Boolean =
        jsonStr.contains("\"infrastructureAsCodeIssues\":") && !jsonStr.contains("\"error\":")

    override fun buildExtraOptions(): List<String> = listOf("--json")
}
