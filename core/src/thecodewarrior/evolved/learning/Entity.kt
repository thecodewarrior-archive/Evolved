package thecodewarrior.evolved.learning

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import thecodewarrior.evolved.EvolveWorld
import thecodewarrior.evolved.plus
import thecodewarrior.evolved.vec

/**
 * Created by TheCodeWarrior
 */
abstract class Entity(val physWorld: World, var pos: Vector2 = Vector2(0f,0f)) : Visible {
    private var body: Body
    var rot: Float = 0f

    var angularVelocity = 0f
    var linearVelocity = vec(0,0)

    var isDead: Boolean = false
        protected set
    var age: Int = 0
        protected set
    var nearRange = 1f

    abstract fun getShapes(): List<Shape>
    abstract fun renderShape(render: ShapeRenderer)
    abstract fun tick()
    abstract fun click(x: Float, y: Float)
    abstract fun entityNear(entity: Entity)

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

    fun render(render: ShapeRenderer) {
        if(this.age <= 10) {
            val l = if(age <= 2) 1000f else 3f
            render.setColor(1f, 1f, 1f, 1f)
            render.rectLine(this.pos.x, this.pos.y-l, this.pos.x, this.pos.y+l, 0.25f)
            render.rectLine(this.pos.x-l, this.pos.y, this.pos.x+l, this.pos.y, 0.25f)
        }

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

        val set = mutableSetOf<Entity>()
        physWorld.QueryAABB({ fixture ->
            val userData = fixture.body.userData
            if(userData is Entity) {
                if(userData != this)
                    set.add(userData)
            }
            return@QueryAABB true
        }, this.pos.x-nearRange, this.pos.y-nearRange, this.pos.x+nearRange, this.pos.y+nearRange)

        set.forEach { entity ->
            if(entity != this) {
                entityNear(entity)
            }
        }
        tick()

        pos += linearVelocity
        rot = (rot + angularVelocity) % 360

        val worldRadius = EvolveWorld.mainWorld.worldSize/2

        if(this.pos.x < -worldRadius)
            this.pos.x += 2*worldRadius
        if(this.pos.x > worldRadius)
            this.pos.x -= 2*worldRadius

        if(this.pos.y < -worldRadius)
            this.pos.y += 2*worldRadius
        if(this.pos.y > worldRadius)
            this.pos.y -= 2*worldRadius


        this.body.setTransform(pos, MathUtils.degreesToRadians * rot)
    }

}
