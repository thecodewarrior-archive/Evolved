package thecodewarrior.evolved.learning

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import thecodewarrior.evolved.vec

/**
 * Created by TheCodeWarrior
 */
abstract class Entity(physWorld: World, var pos: Vector2 = Vector2(0f,0f)) {
    var body: Body
    var rot: Float = 0f

    var isDead: Boolean = false
        protected set
    var age: Int = 0
        protected set

    abstract fun getShapes(): List<Shape>
    abstract fun renderShape(render: ShapeRenderer)
    abstract fun tick()
    abstract fun click(x: Float, y: Float)
    abstract fun  onContact(entity: Entity)

    init {
        val bodyDef = BodyDef()
        bodyDef.type = BodyDef.BodyType.DynamicBody
        bodyDef.position.set(pos)
//        bodyDef.linearDamping = 0.5f
//        bodyDef.angularDamping = 0.5f

        body = physWorld.createBody(bodyDef)
        body.userData = this

        updateShape()
    }

    fun updateShape() {
        while(body.fixtureList.size > 0) { // roundabout to avoid CMEs
            body.destroyFixture(body.fixtureList.first())
        }

        val shapes = getShapes()
        shapes.forEach { shape ->
            val fixtureDef = FixtureDef()
            fixtureDef.shape = shape
            fixtureDef.density = 5f

            val fixture = body.createFixture(fixtureDef)

            shape.dispose()
        }
    }

    fun updateFromPhys() {
        pos = body.position
        rot = MathUtils.radiansToDegrees * body.angle
    }

    fun render(render: ShapeRenderer) {
        updateFromPhys()
        render.translate(pos.x, pos.y, 0f)
        render.rotate(0f, 0f, 1f, rot)

        renderShape(render)

        render.rotate(0f, 0f, -1f, rot)
        render.translate(-pos.x, -pos.y, 0f)
    }

    fun render(batch: SpriteBatch) {

    }

    fun onDelete() {
        body.world.destroyBody(body)
    }

    fun kill() {
        isDead = true
    }

    fun normal(): Vector2 {
        return vec(0, 1).rotate(rot)
    }

    fun rootTick() {
        age++
        tick()
    }

}
