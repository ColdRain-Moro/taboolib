package taboolib.module.kether.action.transform

import io.izzel.kether.common.api.ParsedAction
import io.izzel.kether.common.loader.types.ArgTypes
import taboolib.common5.util.printed
import taboolib.module.kether.Kether.expects
import taboolib.module.kether.KetherParser
import taboolib.module.kether.ScriptAction
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.scriptParser
import java.util.concurrent.CompletableFuture

/**
 * @author IzzelAliz
 */
class ActionPrinted(val date: ParsedAction<*>, val separator: String) : ScriptAction<List<String>>() {

    override fun run(frame: ScriptFrame): CompletableFuture<List<String>> {
        return frame.newFrame(date).run<Any>().thenApply {
            it.toString().printed(separator)
        }
    }

    override fun toString(): String {
        return "ActionPrinted(date=$date, separator='$separator')"
    }

    companion object {

        /**
         * printed *xxx by "_"
         */
        @KetherParser(["printed"])
        fun parser() = scriptParser {
            ActionPrinted(it.next(ArgTypes.ACTION), try {
                it.mark()
                it.expects("by", "with")
                it.nextToken()
            } catch (ignored: Exception) {
                it.reset()
                "_"
            })
        }
    }
}