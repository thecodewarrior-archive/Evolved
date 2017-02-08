package thecodewarrior.evolved.learning

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.Shape
import com.badlogic.gdx.physics.box2d.World
import thecodewarrior.evolved.learning.gene.BlipGenome
import thecodewarrior.evolved.learning.gene.Genome
import thecodewarrior.evolved.times
import thecodewarrior.evolved.vec

/**
 * Created by TheCodeWarrior
 */

class EntityBlip(world: World, pos: Vector2, val genome: Genome = BlipGenome()) : Entity(world, pos) {

    val blipController = BlipController(this, genome)
    var food = 10000

    override fun getShapes(): List<Shape> {
        val shape = PolygonShape()
        shape.set(arrayOf(
                vec(0, 1),
                vec(-1/4f, 0),
                vec(1/4f, 0)
        ))
        return listOf(shape)
    }

    override fun renderShape(render: ShapeRenderer) {
        val a = Math.min(1f, Math.max(0f, food/1000f))
        render.setColor(a, a, a, 1f)
        render.triangle(
                0f, 1f,
                -0.25f, 0f,
                0.25f, 0f
        )

        render.setColor(0f, 1f, 0f, 1f)
        if(blipController.forward > 0) {
            render.triangle(
                    0f, -0.5f,
                    -0.125f, 0f,
                    0.125f, 0f
            )
        }

        if(blipController.torque > 0) {
            render.triangle(
                    0.5f, 1f-0.125f,
                    0f, 1f-0.25f,
                    0f, 1f
            )
        }

        if(blipController.torque < 0) {
            render.triangle(
                    -0.5f, 1f-0.125f,
                    0f, 1f,
                    0f, 1f-0.25f
            )
        }
    }

    override fun tick() {
        food -= 5

        blipController.tick()

        this.body.applyForceToCenter(normal() * blipController.forward, true)
        this.body.applyTorque(blipController.torque, true)

        if(food <= 0)
            this.kill()
    }

    override fun click(x: Float, y: Float) {
        this.kill()
    }

    override fun onContact(entity: Entity) {
        if(entity is EntityFood) {
            entity.food -= 50
            food += 50
        }
    }
}
