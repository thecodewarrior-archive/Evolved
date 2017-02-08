package thecodewarrior.evolved

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import thecodewarrior.evolved.learning.Entity
import thecodewarrior.evolved.learning.EntityBlip
import thecodewarrior.evolved.learning.EntityFood
import java.util.concurrent.ThreadLocalRandom

/**
 * Created by TheCodeWarrior
 */
class EvolveWorld(val game: EvolvedGame) : ContactListener {
    companion object {
        lateinit var mainWorld: EvolveWorld
    }

    var mousePos = vec(0,0)

    val physWorld = World(Vector2(0f,0f), true)
    val worldSize = 50f

    val left: Body
    val right: Body
    val top: Body
    val bottom: Body

    val tl: Body
    val tr: Body
    val bl: Body
    val br: Body

    val entities = mutableListOf<Entity>(
            EntityBlip(physWorld, vec(3, 3))
    )

    init {
        val l = 1000f
        left = createEdge(0f, l)
        right = createEdge(0f, l)

        top = createEdge(l, 0f)
        bottom = createEdge(l, 0f)

        tl = createLine(vec(-1, -3) to vec(3, 1), vec(-1, -6) to vec(1, 0), vec(0, -1) to vec(6, 1))
        bl = createLine(vec(-1, 3) to vec(3, -1), vec(0, 1) to vec(6, -1), vec(1, 0) to vec(-1, 6))
        br = createLine(vec(-3, -1) to vec(1, 3), vec(-6, -1) to vec(0, 1), vec(-1, 0) to vec(1, 6))
        tr = createLine(vec(-3, 1) to vec(1, -3), vec(-6, 1) to vec(0, -1), vec(-1, 0) to vec(1, -6))

        setupEdges(worldSize, worldSize)

        for(i in 0..15) {
            entities.add(EntityBlip(physWorld, vec(
                    ThreadLocalRandom.current().nextDouble(-worldSize / 2.0, worldSize / 2.0),
                    ThreadLocalRandom.current().nextDouble(-worldSize / 2.0, worldSize / 2.0)
            )))
        }
    }

    fun setupEdges(width: Float, height: Float) {
        left.setTransform(vec(-width/2, 0), 0f)
        right.setTransform(vec(width/2, 0), 0f)
        bottom.setTransform(vec(0, -height/2), 0f)
        top.setTransform(vec(0, height/2), 0f)

        tl.setTransform(vec(-width/2, height/2), 0f)
        bl.setTransform(vec(-width/2, -height/2), 0f)

        tr.setTransform(vec(width/2, height/2), 0f)
        br.setTransform(vec(width/2, -height/2), 0f)
    }

    fun createLine(vararg pairs: Pair<Vector2, Vector2>): Body {
        val bodyDef = BodyDef()
        bodyDef.type = BodyDef.BodyType.StaticBody
        val body = physWorld.createBody(bodyDef)

        for(pair in pairs) {
            val fixtureDef = FixtureDef()

            val edgeShape = EdgeShape()
            edgeShape.set(pair.first.x, pair.first.y, pair.second.x, pair.second.y)

            fixtureDef.shape = edgeShape
            fixtureDef.restitution = 1f
            body.createFixture(fixtureDef)

            edgeShape.dispose()
        }

        return body
    }

    fun createEdge(xSize: Float, ySize: Float): Body {
        return createLine(vec(-xSize, -ySize) to vec(xSize, ySize))
    }

    fun draw(render: ShapeRenderer) {
        entities.forEach {
            it.render(render)
        }
    }

    fun draw(batch: SpriteBatch) {
        entities.forEach {
            it.render(batch)
        }
    }

    fun draw(projectionMatrix: Matrix4) {
    }

    fun tick() {
        physWorld.step(1/EvolvedGame.tps, 6, 2)

        physWorld.contactList.forEach { contact ->
            val a = contact.fixtureA.body.userData as? Entity
            val b = contact.fixtureB.body.userData as? Entity

            if(a != null && b != null) {
                a.onContact(b)
                b.onContact(a)
            }
        }

        val remove = mutableListOf<Entity>()
        entities.forEach {
            if(it.isDead) {
                remove.add(it)
                it.onDelete()
                return@forEach
            } else it.rootTick()
        }
        entities.removeAll(remove)

        if(entities.count { it is EntityBlip } < 15) {
            val a = entities.filter { it is EntityBlip && it.age/2 > 100-it.blipController.brainDeadCountdown}.maxBy { it as EntityBlip; it.food } as EntityBlip?
            val b = entities.filter { it is EntityBlip && it.age/2 > 100-it.blipController.brainDeadCountdown && it != a }.maxBy { it as EntityBlip; it.food } as EntityBlip?
            if(a == null || b == null) {
                entities.add(EntityBlip(physWorld, vec(
                        ThreadLocalRandom.current().nextDouble(-worldSize / 2.0, worldSize / 2.0),
                        ThreadLocalRandom.current().nextDouble(-worldSize / 2.0, worldSize / 2.0)
                )))
            } else {
                val genomeA = a.genome
                val genomeB = b.genome

                var newGenome = genomeA.cross(genomeB).mutate()

                entities.add(EntityBlip(physWorld, vec(
                        ThreadLocalRandom.current().nextDouble(-worldSize / 2.0, worldSize / 2.0),
                        ThreadLocalRandom.current().nextDouble(-worldSize / 2.0, worldSize / 2.0)
                ), genomeA))
            }
        }
        if(entities.count { it is EntityFood } < 50) {
            entities.add(EntityFood(physWorld, vec(
                    ThreadLocalRandom.current().nextDouble(-worldSize/2.0, worldSize/2.0),
                    ThreadLocalRandom.current().nextDouble(-worldSize/2.0, worldSize/2.0)
            )))
        }
    }

    fun dispose() {
    }

    fun  click(x: Float, y: Float): Boolean {
        var d = false
        physWorld.QueryAABB({
            val body = it.body

            val e = body.userData as? Entity ?: return@QueryAABB false
            e.click(x, y)
            d = true
            return@QueryAABB false
        }, x, y, x, y)

        if(!d) {
            if(Gdx.input.isButtonPressed(Input.Buttons.LEFT))
                entities.add(EntityFood(physWorld, vec(x, y)))
            if(Gdx.input.isButtonPressed(Input.Buttons.RIGHT))
                entities.add(EntityBlip(physWorld, vec(x, y)))
        }

        return false
    }

    fun updateMouse(x: Float, y: Float) {
        mousePos = vec(x, y)
    }

    override fun endContact(contact: Contact) {
        val a = contact.fixtureA.body.userData as? Entity
        val b = contact.fixtureB.body.userData as? Entity

        if(a != null && b != null) {

        }
    }

    override fun beginContact(contact: Contact) {
        val a = contact.fixtureA.body.userData as? Entity
        val b = contact.fixtureB.body.userData as? Entity

        if(a != null && b != null) {

        }
    }

    override fun preSolve(contact: Contact?, oldManifold: Manifold?) {}

    override fun postSolve(contact: Contact?, impulse: ContactImpulse?) {}

}
