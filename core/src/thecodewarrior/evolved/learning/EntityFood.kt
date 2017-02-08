package thecodewarrior.evolved.learning

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.Shape
import com.badlogic.gdx.physics.box2d.World
import thecodewarrior.evolved.vec
import java.util.concurrent.ThreadLocalRandom

/**
 * Created by TheCodeWarrior
 */
class EntityFood(world: World, pos: Vector2) : Entity(world, pos) {
    companion object {
        val maxFood = 10000
    }

    val r: Float
        get() = food/(maxFood*2) + 0.1f

    var rawFood: Int = maxFood

    var food: Int
        set(value) {
            rawFood = value
            if(value < 0)
                this.kill()
            else {
                updateShape()
            }
        }
        get() = rawFood

    var inited = false
    init {
        rawFood = maxFood
        inited = true
    }

    override fun getShapes(): List<Shape> {
        if(!inited)
            rawFood = maxFood
        val shape = PolygonShape()
        shape.set(arrayOf(
                vec(-r, -r),
                vec(-r, r),
                vec(r, r),
                vec(r, -r)
        ))
        return listOf(shape)
    }

    override fun renderShape(render: ShapeRenderer) {
        render.setColor(0f, 0f, 0.74f, 1f)
        render.triangle(
                r, r,
                -r, -r,
                -r, r
        )
        render.triangle(
                r, r,
                r, -r,
                -r, -r
        )
    }

    override fun tick() {

        val r = ThreadLocalRandom.current()
        if(r.nextFloat() < 0.05f) {
//            this.body.applyForceToCenter(vec(r.nextDouble(2.0)-1, r.nextDouble(2.0)-1) * 300, false)
        }

    }

    override fun click(x: Float, y: Float) {
        this.kill()
    }

    override fun onContact(entity: Entity) {}
}
