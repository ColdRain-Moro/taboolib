package taboolib.module.kether

import io.izzel.kether.common.api.*
import io.izzel.kether.common.loader.QuestReader
import io.izzel.kether.common.loader.SimpleQuestLoader
import io.izzel.kether.common.util.LocalizedException
import taboolib.common.platform.warning
import taboolib.common5.Coerce
import taboolib.module.kether.Kether.expects
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CompletableFuture

typealias Script = Quest

typealias ScriptFrame = QuestContext.Frame

fun <T> scriptParser(resolve: (QuestReader) -> QuestAction<T>): QuestActionParser {
    return object : QuestActionParser, Serializable {

        private val serialVersionUID = 1L

        @Suppress("UNCHECKED_CAST")
        override fun <T> resolve(resolver: QuestReader): QuestAction<T> {
            return resolve.invoke(resolver) as QuestAction<T>
        }

        override fun complete(params: MutableList<String>): MutableList<String> {
            return KetherCompleters.seq(KetherCompleters.consume()).apply(params)
        }
    }
}

fun String.parseKetherScript(namespace: List<String> = emptyList()): Script {
    return SimpleQuestLoader().load(ScriptService, "temp_${UUID.randomUUID()}", toByteArray(StandardCharsets.UTF_8), namespace)
}

fun List<String>.parseKetherScript(namespace: List<String> = emptyList()): Script {
    return joinToString("\n").parseKetherScript(namespace)
}

fun QuestContext.Frame.script(): ScriptContext {
    return context() as ScriptContext
}

fun QuestContext.Frame.deepVars(): HashMap<String, Any?> {
    return HashMap<String, Any?>().also { map ->
        var parent = parent()
        while (parent.isPresent) {
            map.putAll(parent.get().variables().keys().map { it to variables().get<Any>(it).orElse(null) })
            parent = parent.get().parent()
        }
        map.putAll(variables().keys().map { it to variables().get<Any>(it).orElse(null) })
    }
}

fun Throwable.printMessage() {
    if (this is LocalizedException) {
        warning("Unexpected exception while parsing kether script:")
        localizedMessage.split("\n").forEach { warning(it) }
    } else {
        printStackTrace()
    }
}

fun Any?.inferType(): Any? {
    val asInteger = asInteger(this)
    if (asInteger.isPresent) {
        return asInteger.get()
    }
    val asLong = Coerce.asLong(this)
    if (asLong.isPresent) {
        return asLong.get()
    }
    val asDouble = Coerce.asDouble(this)
    if (asDouble.isPresent) {
        return asDouble.get()
    }
    val asBoolean = Coerce.asBoolean(this)
    if (asBoolean.isPresent) {
        return asBoolean.get()
    }
    return this
}

private fun asInteger(obj: Any?): Optional<Int> {
    return when (obj) {
        null -> {
            Optional.empty()
        }
        is Number -> {
            Optional.of(obj.toInt())
        }
        else -> {
            try {
                Optional.ofNullable(Integer.valueOf(obj.toString()))
            } catch (ignored: NumberFormatException) {
                Optional.empty()
            }
        }
    }
}

fun ScriptContext.extend(map: Map<String, Any?>) {
    rootFrame().variables().run {
        map.forEach { (k, v) -> set(k, v) }
    }
}

fun QuestReader.switch(func: ExpectDSL.() -> Unit): ScriptAction<*> {
    val ed = ExpectDSL()
    func(ed)
    return try {
        val sel = expects(*ed.method.keys.toTypedArray())
        ed.method[sel]!!()
    } catch (ex: Exception) {
        if (ed.other == null) {
            throw ex
        }
        ed.other?.invoke(this) ?: throw ex
    }
}

fun actionNow(name: String = "kether-action-del", func: QuestContext.Frame.() -> Any?): ScriptAction<*> {
    return object : ScriptAction<Any?>() {

        override fun run(frame: ScriptFrame): CompletableFuture<Any?> {
            return CompletableFuture.completedFuture(func(frame))
        }

        override fun toString(): String {
            return "KetherDSL($name)"
        }
    }
}

fun actionFuture(name: String = "kether-action-del", func: QuestContext.Frame.(CompletableFuture<Any?>) -> Any?): ScriptAction<*> {
    return object : ScriptAction<Any?>() {

        override fun run(frame: ScriptFrame): CompletableFuture<Any?> {
            val future = CompletableFuture<Any?>()
            func(frame, future)
            return future
        }

        override fun toString(): String {
            return "KetherDSL($name)"
        }
    }
}

class ExpectDSL {

    val method = HashMap<String, QuestReader.() -> ScriptAction<*>>()
    var other: (QuestReader.() -> ScriptAction<*>?)? = null
        private set

    fun case(vararg str: String, func: QuestReader.() -> ScriptAction<*>) {
        str.forEach { method[it] = func }
    }

    fun other(func: QuestReader.() -> ScriptAction<*>?) {
        other = func
    }
}

inline fun <T> Iterable<T>.subBy(selector: (T) -> Int): Int {
    var sum = selector(firstOrNull() ?: return 0)
    forEachIndexed { index, element ->
        if (index > 0) {
            sum -= selector(element)
        }
    }
    return sum
}

inline fun <T> Iterable<T>.subByDouble(selector: (T) -> Double): Double {
    var sum = selector(firstOrNull() ?: return 0.0)
    forEachIndexed { index, element ->
        if (index > 0) {
            sum -= selector(element)
        }
    }
    return sum
}

inline fun <T> Iterable<T>.mulBy(selector: (T) -> Int): Int {
    var sum = 1
    for (element in this) {
        sum *= selector(element)
    }
    return sum
}

inline fun <T> Iterable<T>.mulByDouble(selector: (T) -> Double): Double {
    var sum = 1.0
    for (element in this) {
        sum *= selector(element)
    }
    return sum
}

inline fun <T> Iterable<T>.divBy(selector: (T) -> Int): Int {
    var sum = selector(firstOrNull() ?: return 0)
    forEachIndexed { index, element ->
        if (index > 0) {
            sum /= selector(element)
        }
    }
    return sum
}

inline fun <T> Iterable<T>.divByDouble(selector: (T) -> Double): Double {
    var sum = selector(firstOrNull() ?: return 0.0)
    forEachIndexed { index, element ->
        if (index > 0) {
            sum /= selector(element)
        }
    }
    return sum
}

fun Any.isInt(): Boolean {
    return try {
        Integer.parseInt(toString())
        true
    } catch (ex: Exception) {
        false
    }
}