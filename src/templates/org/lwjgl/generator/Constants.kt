/* 
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */
package org.lwjgl.generator

import java.io.PrintWriter
import java.util.regex.Pattern

// Extension properties for numeric literals.
inline val Int.b: Byte get() = this.toByte()
inline val Int.s: Short get() = this.toShort()
inline val Long.i: Int get() = this.toInt()

abstract class ConstantType<T>(
	val javaType: Class<T>
) {
	abstract fun print(value: T): String
	abstract fun nullValue(): T
}

val ByteConstant = object: ConstantType<Byte>(javaClass()) {
	override fun print(value: Byte) = "0x%X".format(value.toInt())
	override fun nullValue() = 0.b
}

val ShortConstant = object: ConstantType<Short>(javaClass()) {
	override fun print(value: Short) = "0x%X".format(value.toInt())
	override fun nullValue() = 0.s
}

val IntConstant = object: ConstantType<Int>(javaClass()) {
	override fun print(value: Int) = "0x%X".format(value)
	override fun nullValue() = 0
}

val LongConstant = object: ConstantType<Long>(javaClass()) {
	override fun print(value: Long) = "0x%XL".format(value)
	override fun nullValue() = 0L
}

val FloatConstant = object: ConstantType<Float>(javaClass()) {
	override fun print(value: Float) = "%sf".format(value)
	override fun nullValue() = 0.0f
}

class ConstantBlock<T>(
	val nativeClass: NativeClass,
	val constantType: ConstantType<T>,
	val documentation: String,
	vararg val constants: Constant<T>
) {

	private var noPrefix = false

	fun noPrefix() {
		noPrefix = true
	}

	private fun getConstantName(name: String) = if ( noPrefix ) name else "${nativeClass.prefixConstant}$name"

	fun generate(writer: PrintWriter) {
		writer.generateBlock()
	}

	private fun PrintWriter.generateBlock() {
		println(documentation)

		println("\tpublic static final ${constantType.javaType.getSimpleName()}")

		// Find maximum constant name length
		val alignment = constants.map {
			it.name.size
		}.fold(0) {(left, right) ->
			Math.max(left, right)
		}

		constants.forEachWithMore { (it, more) ->
			if ( more )
				println(',')
			printConstant(it, alignment)
		}
		println(";\n")
	}

	private fun PrintWriter.printConstant(constant: Constant<T>, alignment: Int) {
		val (name, value) = constant

		print("\t\t${getConstantName(name)}")
		for ( i in 0..(alignment - name.size - 1) )
			print(' ')

		print(" = ")
		if ( constant is ConstantExpression )
			print(constant.expression)
		else
			print(constantType.print(value!!))
	}

	fun toJavaDocLinks(global: Boolean = false): String {
		val builder = StringBuilder(constants.size * 32)

		printJavaDocLink(builder, constants[0], global)
		for ( i in 1..constants.lastIndex ) {
			builder append " "
			printJavaDocLink(builder, constants[i], global)
		}

		return builder.toString()
	}

	private fun <T> printJavaDocLink(builder: StringBuilder, constant: Constant<T>, global: Boolean = false) {
		if ( global )
			builder append nativeClass.className
		builder append '#'
		builder append constant.name
	}

}

open data class Constant<T: Any>(val name: String, val value: T?)
class ConstantExpression<T>(name: String, val expression: String): Constant<T>(name, null)

/** Adds a new constant. */
fun <T> String._(value: T) = Constant(this, value)

/** Adds a new constant whose value is a function of previously defined constants. */
fun <T> String.expr(expression: String) = ConstantExpression<T>(this, expression)
