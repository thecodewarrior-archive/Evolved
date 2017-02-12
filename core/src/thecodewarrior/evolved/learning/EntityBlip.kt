package thecodewarrior.evolved.learning

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.Shape
import com.badlogic.gdx.physics.box2d.World
import thecodewarrior.evolved.EvolveWorld
import thecodewarrior.evolved.learning.genetics.BlipGenome
import thecodewarrior.evolved.times
import thecodewarrior.evolved.vec
import java.util.concurrent.ThreadLocalRandom

/**
 * Created by TheCodeWarrior
 */

class EntityBlip(world: World, pos: Vector2, val genome: BlipGenome = BlipGenome()) : Entity(world, pos) {

    override val color: DeepColor = DeepColor().with(DeepChannel.CREATURE, 1.0)

    val blipController = BlipController(this, genome)
    val maxFood = 20000
    var food = 10000 + ThreadLocalRandom.current().nextInt(1000)
    var children = mutableSetOf<EntityBlip>()

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

        if(EvolveWorld.mainWorld.selected == this) {
            render.setColor(0f, 0f, 0.5f, 0.1f)
            val w = 0.1f
            render.rectLine(
                    0f, 1f,
                    -0.25f, 0f,
                    w
            )

            render.rectLine(
                    -0.25f, 0f,
                    0.25f, 0f,
                    w
            )

            render.rectLine(
                    0.25f, 0f,
                    0f, 1f,
                    w
            )

            val interval = 6f
            var angle = (blipController.genome.mainData.fov*interval).toInt() / interval

            var a = 0f
            while(a <= angle) {

                var pos: Vector2 = this.normal().rotate(a-angle/2).scl(10000f)
                var frac = 1f
                physWorld.rayCast(cast@{ f, point, normal, d ->
                    if(f.body.userData != this && f.body.userData is Visible) {
                        if(d < frac) {
                            frac = d
                        }
                    }
                    return@cast frac
                }, this.pos, pos.cpy())
                pos.scl(frac)
                pos.rotate(-this.rot)

                render.rectLine(vec(0, 0), pos, w)
                a += interval
            }
        }

        val a = Math.min(1f, Math.max(0f, food/1000f))
        render.setColor(1f, 1f, 1f, a)
        render.triangle(
                0f, 1f,
                -0.25f, 0f,
                0.25f, 0f
        )
        if(blipController.breedWillingness > -0.1) {
            render.setColor(0f, 0f, 1f, a)
            render.triangle(
                    0f, 0.5f,
                    -0.125f, 0.125f,
                    0.125f, 0.125f
            )
        }

        render.setColor(0f, 1f, 0f, a)
        if(blipController.forward > 0) {
            render.triangle(
                    0f, -0.5f,
                    -0.125f, 0f,
                    0.125f, 0f
            )
        }

//        val torqueFrac = blipController.torque / blipController.maxTorquePower
//        val thrusterLen = Math.abs(torqueFrac)*0.5f
//        if(torqueFrac > 0) {
//            render.triangle(
//                    thrusterLen, 1f-0.125f,
//                    0f, 1f-0.25f,
//                    0f, 1f
//            )
//        }
//
//        if(torqueFrac < 0) {
//            render.triangle(
//                    thrusterLen, 1f-0.125f,
//                    0f, 1f,
//                    0f, 1f-0.25f
//            )
//        }
    }

    override fun tick() {
        children.removeAll { it.isDead }

        food -= 10

        blipController.tick()

        linearVelocity = normal() * blipController.forward
        angularVelocity = blipController.torque

//        this.body.applyForceToCenter(normal() * blipController.forward, true)
//        this.body.applyTorque(blipController.torque, true)
//
//        val maxLinear = 20
//        val maxAngular = 3f
//
//        val len = this.body.linearVelocity.len()
//        if(len > 15)
//            this.body.linearVelocity.scl(15/len)
//
//        if(this.body.angularVelocity > maxAngular)
//            this.body.angularVelocity = maxAngular
//        if(this.body.angularVelocity < -maxAngular)
//            this.body.angularVelocity = -maxAngular
        if(food <= 0)
            this.kill()

    }

    override fun click(x: Float, y: Float) {
        this.kill()
    }

    override fun entityNear(entity: Entity) {
        if (entity is EntityBlip) {
            if (blipController.breedCooldown <= 0 && entity.blipController.breedCooldown <= 0 && blipController.breedWillingness > -0.1 && entity.blipController.breedWillingness > -0.1) {
                EvolveWorld.mainWorld.breedingPairs.add(this to entity)
                this.blipController.breedCooldown = 50 + ThreadLocalRandom.current().nextInt(50)
                entity.blipController.breedCooldown = 50 + ThreadLocalRandom.current().nextInt(50)
            }
        }
        val toAbsorb = (25 * this.linearVelocity.len()).toInt()
        if(entity is EntityFood && food <= maxFood-toAbsorb) {
            entity.food -= toAbsorb
            food += toAbsorb
        }
    }

    fun childCount(): Int {
        if(children.size == 0)
            return 0
        else
            return children.size + children.sumBy { it.childCount() }
    }
}
