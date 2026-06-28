package com.awakenedredstone.prechecks

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException

@SupportedAnnotationTypes("com.awakenedredstone.prechecks.RequireFieldsFrom")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
class InheritanceChecker : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement?>, roundEnv: RoundEnvironment): Boolean {
        for (annotation in annotations) {
            for (classElement in roundEnv.getElementsAnnotatedWith(annotation)) {
                val fieldsFrom = classElement.getAnnotation(RequireFieldsFrom::class.java)!!

                val sourceClass: Element
                try {
                    fieldsFrom.value.java
                    throw IllegalStateException("This should never be thrown!")
                } catch (e: MirroredTypeException) {
                    sourceClass = (e.typeMirror as DeclaredType).asElement()
                }

                val fields = sourceClass.enclosedElements.filter { (getFieldModifiers(it) and fieldsFrom.modifiers) != 0 }.map { it.simpleName.toString() }.toMutableList()
                val elements = classElement.enclosedElements.filter { it.kind.isField }.map { it.simpleName.toString() }

                fields.removeAll(elements)

                if (!fields.isEmpty()) {
                    error("${classElement.simpleName} is missing fields from ${sourceClass.simpleName}${fields.joinToString("\n\t- ", "\n\t- ")}")
                }
            }
        }

        return true
    }

    fun getFieldModifiers(element: Element): Int {
        val modifiers = element.modifiers
        var flags = 0

        for (modifier in modifiers) {
            flags = flags or when (modifier) {
                Modifier.PUBLIC -> java.lang.reflect.Modifier.PUBLIC
                Modifier.PROTECTED -> java.lang.reflect.Modifier.PROTECTED
                Modifier.PRIVATE -> java.lang.reflect.Modifier.PRIVATE
                Modifier.STATIC -> java.lang.reflect.Modifier.STATIC
                Modifier.FINAL -> java.lang.reflect.Modifier.FINAL
                Modifier.TRANSIENT -> java.lang.reflect.Modifier.TRANSIENT
                Modifier.VOLATILE -> java.lang.reflect.Modifier.VOLATILE
                else -> 0
            }
        }

        return flags
    }
}
