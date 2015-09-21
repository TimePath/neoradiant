package com.timepath.q3map

import com.jme3.app.SimpleApplication
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.util.BufferUtils
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D


fun Vector3f.plus() = this
fun Vector3f.plus(other: Vector3f) = this add other
fun Vector3f.minus() = this.negate()
fun Vector3f.minus(other: Vector3f) = this subtract other
fun Vector3f.times(other: Vector3f) = this mult other
fun Vector3f.times(other: Float) = this mult other
fun Vector3f.div(other: Vector3f) = this divide other
fun Vector3f.div(other: Float) = this divide other

fun FloatBuffer(vararg data: Vector3f) = BufferUtils.createFloatBuffer(*data)
fun ShortBuffer(vararg data: Short) = BufferUtils.createShortBuffer(*data)

fun Vector3D.to3f() = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
fun Vector3D.plus() = this
fun Vector3D.plus(other: Vector3D) = this add other
fun Vector3D.minus() = negate()
fun Vector3D.minus(other: Vector3D) = this subtract other
fun Vector3D.times(other: Double) = this scalarMultiply other
fun Vector3D.div(other: Double) = this scalarMultiply 1 / other

inline fun List<T>.choose3<T>(f: (T, T, T) -> Unit) {
    val len = size() - 1
    for (i in 0..len - 2) {
        for (j in i + 1..len - 1) {
            for (k in j + 1..len) {
                f(this[i], this[j], this[k])
            }
        }
    }
}

fun SimpleApplication.solidColor(c: ColorRGBA): Material {
    val s = "Common/MatDefs/Light/Lighting.j3md"
    return Material(assetManager, s) apply {
        setBoolean("UseMaterialColors", true)
        setColor("Ambient", c)
        setColor("Diffuse", c)
        setBoolean("VertexLighting", false)
    }
}

fun SimpleApplication.fullbrightColor(c: ColorRGBA): Material {
    val s = "Common/MatDefs/Misc/Unshaded.j3md"
    return Material(assetManager, s) apply {
        setColor("Color", c)
    }
}
