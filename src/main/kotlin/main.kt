import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file

class ExternalSchemaDemo : CliktCommand() {
    private val script by option().file(mustBeReadable = true).required()
    private val schema by option().file(mustBeReadable = true).required()

    override fun run() {
        interpretFromFiles(script, schema)
    }
}

fun main(args: Array<String>) = ExternalSchemaDemo().main(args)
