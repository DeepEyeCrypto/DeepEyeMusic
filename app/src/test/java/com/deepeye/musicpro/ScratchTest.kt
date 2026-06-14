package com.deepeye.musicpro

import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.junit.Test
import java.lang.reflect.Modifier

class ScratchTest {
    @Test
    fun testReflection() {
        val clazz = StreamExtractor::class.java

        println("=== Class Hierarchy ===")
        var current: Class<*>? = clazz
        while (current != null) {
            println("Class: ${current.name}")
            current = current.superclass
        }

        println("\n=== All Methods ===")
        clazz.methods.forEach { method ->
            val modifiers = Modifier.toString(method.modifiers)
            println(
                "$modifiers ${method.returnType.name} ${method.name}(${method.parameterTypes.joinToString { it.name }})"
            )
        }
    }
}
