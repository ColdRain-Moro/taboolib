package taboolib.internal

import org.tabooproject.reflex.ClassField
import org.tabooproject.reflex.ClassMember
import org.tabooproject.reflex.ClassMethod
import taboolib.common.inject.Bind
import taboolib.common.inject.Injector

/**
 * TabooLib
 * taboolib.internal.RegisteredInjector
 *
 * @author 坏黑
 * @since 2022/1/24 7:14 PM
 */
@Internal
class RegisteredInjector(val injector: Injector) {

    val type: List<Class<*>>
    val target: Bind.Target
    val annotation: List<Class<out Annotation>>

    init {
        if (injector.javaClass.isAnnotationPresent(Bind::class.java)) {
            val bind = injector.javaClass.getAnnotation(Bind::class.java)
            type = bind.type.map { it.java }
            target = bind.target
            annotation = bind.value.map { it.java }
        } else {
            type = emptyList()
            target = Bind.Target.ALL
            annotation = emptyList()
        }
    }

    fun checkType(type: Class<*>) =
        this.type.isEmpty() || this.type.any { it.isAssignableFrom(type) }

    fun checkTarget(target: Bind.Target) =
        this.target == Bind.Target.ALL || this.target == target

    fun check(target: Class<*>) =
        (annotation.isEmpty() || annotation.anyAnnotated(target) && checkType(target))

    fun check(target: ClassField) =
        (annotation.isEmpty() || annotation.anyAnnotated(target)) && checkType(target.fieldType)

    fun check(target: ClassMethod) =
        annotation.isEmpty() || annotation.anyAnnotated(target)

    private fun List<Class<out Annotation>>.anyAnnotated(target: ClassMember) =
        this.any { target.isAnnotationPresent(it) }

    private fun List<Class<out Annotation>>.anyAnnotated(target: Class<*>) =
        this.any { target.isAnnotationPresent(it) }
}
