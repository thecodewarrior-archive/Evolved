package thecodewarrior.evolved

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.box2d.*
import org.neuroph.core.Neuron
import thecodewarrior.evolved.learning.*
import java.util.concurrent.ThreadLocalRandom

/**
 * Created by TheCodeWarrior
 */
class EvolveWorld(val game: EvolvedGame) : ContactListener {
    companion object {
        lateinit var mainWorld: EvolveWorld
    }

    private data class NeuronHover(val neuron: Neuron, val pos: Vector2, val desc: String, val data: Any)


    var selected: Entity? = null
    var manualSelect = false
    private var hoveredNeuron: NeuronHover? = null
    var mousePos = vec(0,0)
    var hudMousePos = vec(0,0)

    val breedingPairs = mutableSetOf<Pair<EntityBlip, EntityBlip>>()
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

    val font = BitmapFont()
    var foodTimer = 0



    init {
        val l = 1000f
        left = createEdge(0f, l)
        right = createEdge(0f, l)

        top = createEdge(l, 0f)
        bottom = createEdge(l, 0f)

        tl = createLine()//vec(-1, -3) to vec(3, 1), vec(-1, -6) to vec(1, 0), vec(0, -1) to vec(6, 1))
        bl = createLine()//vec(-1, 3) to vec(3, -1), vec(0, 1) to vec(6, -1), vec(1, 0) to vec(-1, 6))
        br = createLine()//vec(-3, -1) to vec(1, 3), vec(-6, -1) to vec(0, 1), vec(-1, 0) to vec(1, 6))
        tr = createLine()//vec(-3, 1) to vec(1, -3), vec(-6, 1) to vec(0, -1), vec(-1, 0) to vec(1, -6))

        setupEdges(worldSize+0.5f, worldSize+0.5f)
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
//            fixtureDef.restitution = 0.5f
            body.createFixture(fixtureDef)
            body.userData = object : Visible { override val color: DeepColor = DeepColor().with(DeepChannel.BORDER, 1.0) }
            edgeShape.dispose()
        }

        return body
    }

    fun createEdge(xSize: Float, ySize: Float): Body {
        return createLine(vec(-xSize, -ySize) to vec(xSize, ySize))
    }

    fun draw(render: ShapeRenderer) {
        render.setColor(1f, 0f, 0f, 1f)
        render.rect(-worldSize/2, -worldSize/2, worldSize, worldSize)
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

    fun drawHUD(render: ShapeRenderer) {
        hoveredNeuron = null
        var sel = selected
        if(sel is EntityBlip) {
            var origin = vec(128, 200)
            val diagramWidth = 128
            val layerSpace = 32
            val neuronSpace = 32
            val neuronRadius = 4f
            val graphSpacing = layerSpace * (2)

            sel.blipController.genome.brain.nets.forEach { net ->
                val posMap = mutableMapOf<Neuron, Vector2>()
                var y = origin.y
                net.neurons.layers.forEachIndexed { iLayer, layer ->
                    val count = layer.neuronsCount
                    var x = (diagramWidth - neuronSpace*(count-1f))/2 + 16
                    layer.neurons.forEach {
                        posMap[it] = vec(x, y)
                        x += neuronSpace
                    }
                    y += layerSpace
                }
                y = origin.y
                net.neurons.layers.forEachIndexed { iLayer, it ->
                    if(iLayer == 0)
                        render.setColor(0f, 0.5f, 0f, 1f)
                    else if(iLayer == net.neurons.layers.size-1)
                        render.setColor(0.5f, 0f, 0f, 1f)
                    else
                        render.setColor(0f, 0f, 0.75f, 1f)
                    render.rect(16f, y + layerSpace/2, diagramWidth.toFloat(), 1-layerSpace.toFloat())
                    y += layerSpace
                    it.neurons.forEach {
                        it.inputConnections.forEach {
                            val from = posMap[it.fromNeuron]
                            val to = posMap[it.toNeuron]
                            if(it.weight.value < 0)
                                render.setColor(0.75f, 0f, 0f, 1f)
                            else
                                render.setColor(0f, 0.75f, 0f, 1f)

                            if(from != null && to != null)
                                render.rectLine(from, to, (2f * it.weight.value.toFloat()).clamp(1f, 10f))
                        }
                    }
                }

                posMap.forEach {
                    val (neuron, pos) = it

                    if(pos.cpy().sub(hudMousePos).len2() <= neuronRadius*neuronRadius) {
                        var desc = "%.2f - ".format(neuron.output)
                        var data = Any()

                        val inputIndex = net.neurons.inputNeurons.indexOf(neuron)
                        val outputIndex = net.neurons.outputNeurons.indexOf(neuron)

                        if(inputIndex >= 0) {
                            data = net.inputs[inputIndex]
                            desc += "${data.first}@${data.second}"
                        }
                        if(outputIndex >= 0) {
                            data = net.outputs[outputIndex]
                            desc += data.name
                        }

                        hoveredNeuron = NeuronHover(neuron, pos, desc, data)
                    }

                    val s = (neuron.output.toFloat() + 0.1f).clamp(-1f, 1f)
                    render.setColor(0.75f, 0.75f, 0.75f, 1f)
                    render.circle(pos.x, pos.y, neuronRadius + 1f)
                    if(s < 0)
                        render.setColor(-s, 0f, 0f, 1f)
                    else
                        render.setColor(0f, s, 0f, 1f)
                    render.circle(pos.x, pos.y, neuronRadius)
                }

                origin = vec(origin.x, y + graphSpacing)
            }


            val visionHeight = 16f
            val visionNameWidth = 128f + 1
            val visionWidth = 1024f + 1
            val vision = sel.blipController.vision
            val realVision = sel.blipController.realVision
            val realResolution = sel.blipController.genome.mainData.visionResolution

            val visionPxWidth = (visionWidth-1) / vision.size
            val realVisionPxWidth = (visionWidth-1) / realResolution

            var pos = vec(0, Gdx.graphics.height-32)

            DeepChannel.values().forEach { channel ->
                render.color = channel.realColor

                render.rect(pos.x, pos.y, visionNameWidth, visionHeight)

                render.color = Color.ORANGE
                render.rect(pos.x+visionNameWidth, pos.y, visionWidth, visionHeight)

                render.color = Color.WHITE
                var x = pos.x+visionNameWidth+1
                var h = (visionHeight-2)/2 - 1
                var y = pos.y + 1

                realVision[channel]?.forEach {
                    val c = it.toFloat()
                    render.setColor(c, c, c, 1f)

                    render.rect(x, y, realVisionPxWidth-1, h)
                    x += realVisionPxWidth
                }

                x = pos.x+visionNameWidth+1
                y += h+1
                vision.forEach {
                    val c = it[channel].toFloat()
                    render.setColor(c, c, c, 1f)

                    render.rect(x, y, visionPxWidth-1, h)
                    x += visionPxWidth
                }

                hoveredNeuron?.let {
                    if(it.data is Pair<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        val d = it.data as Pair<EnumSense, Double>
                        if(EnumSense.byChannel[channel] == d.first) {
                            render.setColor(1f, 0f, 0f, 0.75f)
                            render.rect(visionNameWidth + visionWidth*d.second.toFloat(), y-h-1, 2f, visionHeight-2)
                        }
                    }
                }

                pos.y -= visionHeight + 2
            }

            run {

                render.color = Color.ORANGE
                render.rect(pos.x, pos.y, visionWidth+visionNameWidth, visionHeight)

                render.color = Color.WHITE
                var y = pos.y + 1
                var x = pos.x+1
                var px = (visionWidth+visionNameWidth-2)/vision.size

                vision.forEach {
                    var colors = mutableListOf<Vector3>()
                    it.channels.forEachIndexed { i, v ->
                        if(v == 0.0)
                            return@forEachIndexed
                        val c = DeepChannel.values()[i].realColor
                        colors.add(Vector3((c.r * v).toFloat(), (c.g * v).toFloat(), (c.b * v).toFloat()))
                    }
                    val avg = colors.fold(Vector3()) { sofar, color -> sofar.add(color) }.div(colors.size)

                    render.setColor(avg.x.clamp(0f, 1f), avg.y.clamp(0f, 1f), avg.z.clamp(0f, 1f), 1f)

                    render.rect(x, y, px, visionHeight-2)
                    x += px
                }
                pos.y -= visionHeight + 2
            }
        }
    }

    fun drawHUD(batch: SpriteBatch) {
        val sel = selected
        if(sel != null && sel is EntityBlip) {
            var _y = 50f
            fun y(): Float {
                _y += 15f
                return _y-15f
            }

            font.draw(batch, "F: ${sel.food} C: ${sel.childCount()}", 32f, y())
            font.draw(batch, "A: ${sel.age}/${sel.blipController.genome.mainData.maxAge}", 32f, y())
            selected?.let {
                if(it is EntityBlip) {
                    EnumAction.values().forEach { action ->
                        font.draw(batch, "$action = ${it.blipController.genome.brain[action]}", 32f, y())
                    }
                }
            }

            hoveredNeuron?.let {
                font.draw(batch, it.desc, 32f, y())
            }

            var pos = vec(0, Gdx.graphics.height-32)
            val visionHeight = 16f

            DeepChannel.values().forEach {
                font.draw(batch, it.name, pos.x, pos.y+14)
                pos.y -= visionHeight+2
            }
        }
        font.draw(batch, "${entities.count { it is EntityBlip } }", 32f, 32f)
    }

    fun drawHUD(projectionMatrix: Matrix4) {
    }

    fun fittest(exclude: EntityBlip? = null): EntityBlip? {
        return entities
                .filter {
                    it is EntityBlip
                    && it != exclude
                    && it.genome.brain.nets.size != 0
                }
                .maxBy {
                    it as EntityBlip
                    it.childCount() * 1000000 + it.food
                } as EntityBlip?
    }

    fun tick() {
        foodTimer--
//        physWorld.step(1/EvolvedGame.tps, 6, 2)
//
//        physWorld.contactList.forEach { contact ->
//            val a = contact.fixtureA.body.userData as? Entity
//            val b = contact.fixtureB.body.userData as? Entity
//
//            if(a != null && b != null) {
//                a.onContact(b)
//                b.onContact(a)
//            }
//        }

        val remove = mutableListOf<Entity>()
        entities.forEach {
            if(it.isDead) {
                remove.add(it)
                it.onDelete()
                return@forEach
            } else it.rootTick()
        }
        entities.removeAll(remove)

        val bred = mutableSetOf<Pair<EntityBlip, EntityBlip>>()
        breedingPairs.forEach {
            if(it in bred)
                return@forEach
            bred.add(it.second to it.first) // to avoid a <-> b and b <-> a duplication

            val energy = it.first.food/2 + it.second.food/2
            it.first.food -= it.first.food/16
            it.second.food -= it.second.food/16

            val genomeA = it.first.genome
            val genomeB = it.second.genome

            var newGenome = genomeA.breed(genomeB)

            var newBlip = EntityBlip(physWorld, it.first.pos.cpy().add(it.second.pos).scl(1/2f), newGenome)
            newBlip.food = energy

            it.first.children.add(newBlip)
            it.second.children.add(newBlip)

            entities.add(newBlip)
        }
        breedingPairs.clear()
        if(entities.count { it is EntityBlip } == 0) {
            400.times {
                entities.add(EntityBlip(physWorld, vec(
                        ThreadLocalRandom.current().nextDouble(-worldSize / 2.0, worldSize / 2.0),
                        ThreadLocalRandom.current().nextDouble(-worldSize / 2.0, worldSize / 2.0)
                )))
            }
        }
        if(entities.count { it is EntityFood } < 50 && foodTimer <= 0) {
            entities.add(EntityFood(physWorld, vec(
                    ThreadLocalRandom.current().nextDouble(-worldSize/2.0 + 3, worldSize/2.0 - 3),
                    ThreadLocalRandom.current().nextDouble(-worldSize/2.0 + 3, worldSize/2.0 - 3)
            )))
//            foodTimer = 75
        }

        if(!manualSelect)
            selected = fittest()
    }

    fun dispose() {
    }

    fun click(x: Float, y: Float): Boolean {
        var d = false

        manualSelect = false

        physWorld.QueryAABB({
            val body = it.body

            val e = body.userData as? Entity ?: return@QueryAABB false

            if(Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                selected = e
                manualSelect = true
                d = true
                return@QueryAABB false
            }

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

    fun updateMouse(x: Float, y: Float, hudX: Float, hudY: Float) {
        mousePos = vec(x, y)
        hudMousePos = vec(hudX, hudY)
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
