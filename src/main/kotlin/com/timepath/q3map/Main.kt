package com.timepath.q3map

import com.jme3.app.SimpleApplication
import com.jme3.light.PointLight
import com.jme3.material.RenderState
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Mesh
import com.jme3.scene.Node
import com.jme3.scene.VertexBuffer
import com.jme3.system.AppSettings
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.apache.commons.math3.geometry.euclidean.threed.Plane
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import java.io.File

object Config {
    val tolerance = 1.0e-10
    val scale = 64f

    val hiddenSurfaces = setOf(
            "common/caulk",
            "common/clip",
            "common/donotenter",
            "common/hint"
    )
}

data class Entity(val brushes: List<Entity.Brush>,
                  val patches: List<Entity.Patch>,
                  val properties: Map<String, String>) {
    data class Patch(
            /** Column then row */
            val matrix: List<List<Vector3D>>,
            val texture: String)

    data class Brush(val faces: List<Brush.Face>) {
        data class Face(val plane: Plane,
                        val texture: String)

        override fun toString() = "{\n${faces.joinToString("\n")}\n}"
    }

    override fun toString() = "{\n${(properties.map { "\"${it.key}\" \"${it.value}\"" } + brushes).joinToString("\n")}\n}"

    companion object {
        fun parse(s: String): List<Entity> {
            val parser = MapParser(CommonTokenStream(MapLexer(ANTLRInputStream(s))))
            return parser.parse().entity().map {
                val properties = it.property().map {
                    val (key, value) = it.String().map { it.text }
                    key to value
                }.toMap()
                val oldbrushes = it.brush().map {
                    Brush(it.plane().map {
                        val points = it.triple().map {
                            val (x, y, z) = it.Number().map { it.text.toDouble() }
                            Vector3D(x, y, z)
                        }
                        val texture = it.File().text
                        check(points.size() == 3)
                        Brush.Face(Plane(points[0], points[1], points[2], Config.tolerance), texture)
                    })
                }
                val brushes = it.brushdef().map {
                    Brush(it.brushdefFragment().map {
                        val (p1, p2, p3, u, v) = it.triple().map {
                            val (x, y, z) = it.Number().map { it.text.toDouble() }
                            Vector3D(x, y, z)
                        }
                        val texture = it.File().text
                        Brush.Face(Plane(p1, p2, p3, Config.tolerance), texture)
                    })
                }
                val patches = it.patchdef().map {
                    val texture = it.File().text
                    val (nCols, nRows) = it.quintuple().Number().map { it.text.toInt() }
                    val matrix = it.patchdefFragment().map { col ->
                        col.quintuple().map { row ->
                            val (x, y, z) = row.Number().map { it.text.toDouble() }
                            Vector3D(x, y, z)
                        }
                    }
                    check(matrix.size() == nCols)
                    check(matrix.all { it.size() == nRows })
                    Patch(matrix, texture)
                }
                Entity(oldbrushes + brushes, patches, properties)
            }
        }
    }
}

fun main(args: Array<String>) = object : SimpleApplication() {
    init {
        showSettings = false
        settings = AppSettings(true) apply {
            title = "NeoRadiant"
            isVSync = false
            frameRate = 0
        }
    }

    val matcolors: MutableMap<String, ColorRGBA> = hashMapOf()

    val pointlight = PointLight() apply {
        color = ColorRGBA.White
        radius = Config.scale * 10
    }

    override fun simpleUpdate(tpf: Float) {
        pointlight.position = camera.location
    }

    override fun simpleInitApp() {
        flyCam.moveSpeed = Config.scale * 1
        val map = File(args.single()).readText()
        val entities = Entity.parse(map)
        val world = entities.first()
        rootNode.attachChild(Node("world") apply {
            attachChild(world)
            scale(1 / Config.scale)
            rotateUpTo(-Vector3f.UNIT_Z)
        })
        rootNode.addLight(pointlight)
    }

    fun Node.attachChild(entity: Entity) {
        for (brush in entity.brushes) {
            attachChild(brush)
        }
        for (patch in entity.patches) {
            attachChild(patch)
        }
    }

    fun Node.attachChild(brush: Entity.Brush) {
        val planepoints: Map<Entity.Brush.Face, List<Vector3D>> = linkedMapOf<Entity.Brush.Face, MutableList<Vector3D>>() apply {
            brush.faces.choose3 { a, b, c ->
                Plane.intersection(a.plane, b.plane, c.plane)?.let { x ->
                    if (brush.faces.any { it.plane.getOffset(x) < -Config.tolerance }) {
                        // Discard points outside inward facing planes, they don't contribute
                    } else {
                        getOrPut(a, { linkedListOf() }).add(x)
                        getOrPut(b, { linkedListOf() }).add(x)
                        getOrPut(c, { linkedListOf() }).add(x)
                    }
                }
            }
        }
        for ((plane, points) in planepoints) {
            if (plane.texture in Config.hiddenSurfaces) continue
            val normal = plane.plane.normal
            val midpoint = points.fold(Vector3D.ZERO) { acc, vec -> acc + vec } / points.size().toDouble()
            val verts = points.sortedWith(comparator { a, b ->
                // Sort in a circle
                val determinant = normal dotProduct ((a - midpoint) crossProduct (b - midpoint))
                when {
                    determinant > 0 -> +1
                    determinant < 0 -> -1
                    else -> 0
                }
            })
            check(verts.size() >= 3)
            attachChild(Geometry("face") apply {
                material = matcolors.getOrPut(plane.texture, { ColorRGBA.randomColor() }) let { solidColor(it) }
                mesh = Mesh() apply {
                    setBuffer(VertexBuffer.Type.Position, 3, FloatBuffer(*verts.map { it.to3f() }.toTypedArray()))
                    setBuffer(VertexBuffer.Type.Index, 3, ShortBuffer(*ShortArray(3 + (verts.size() - 3) * 3) apply {
                        // We have a convex polygon, triangulate with lines from a single vertex to every other
                        var i = 0
                        for (vert in 1..verts.size() - 2) {
                            this[i++] = 0
                            this[i++] = (vert + 0).toShort()
                            this[i++] = (vert + 1).toShort()
                        }
                    }))
                    val norm = normal.negate().to3f()
                    setBuffer(VertexBuffer.Type.Normal, 3, FloatBuffer(*Array(verts.size(), { norm })))
                    updateBound()
                }
            })
        }
    }

    fun Node.attachChild(patch: Entity.Patch) {
        // TODO: bezier mesh rendering
        val columns = patch.matrix
        for (row in columns) {
            attachChild(Geometry("row") apply {
                material = fullbrightColor(ColorRGBA.randomColor()) apply {
                    additionalRenderState.faceCullMode = RenderState.FaceCullMode.Off
                }
                mesh = Mesh() apply {
                    mode = Mesh.Mode.LineStrip
                    lineWidth = 4f
                    setBuffer(VertexBuffer.Type.Position, 3, FloatBuffer(*row.map { it.to3f() }.toTypedArray()))
                    updateBound()
                }
            })
        }
    }
}.start()
