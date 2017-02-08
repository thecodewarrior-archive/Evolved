package thecodewarrior.evolved.learning

import thecodewarrior.evolved.EvolveWorld
import thecodewarrior.evolved.learning.gene.BlipGenes
import thecodewarrior.evolved.learning.gene.Genome
import thecodewarrior.evolved.learning.network.Action
import thecodewarrior.evolved.learning.network.Brain
import thecodewarrior.evolved.learning.network.BrainData
import thecodewarrior.evolved.learning.network.Sense

/**
 * Created by TheCodeWarrior
 */
class BlipController(val blip: EntityBlip, val genome: Genome) {
    var thrusterPower = 5f
    var torquePower = 3f

    var forward = 0f
        private set

    var torque = 0f
        private set

    var brainDeadCountdown = 100

    val brain = Brain(genome[BlipGenes.BRAIN] ?: BrainData.random(), { blip.food -= it })

    init {

    }

    fun tick() {
        brain.tick()

        forward = 0f
        torque = 0f

        val normal = blip.normal()
        val mouseNormal = EvolveWorld.mainWorld.entities.minBy {
            (it as? EntityFood)?.pos?.cpy()?.sub(blip.pos)?.len2() ?: 1000f
        }?.let {
           if(it is EntityFood)
               it.pos.cpy().sub(blip.pos).nor()
           else
               null
        }

        val sound = IntArray(Sense.SOUND.indexRange.last - Sense.SOUND.indexRange.first + 1)

        if(mouseNormal != null) {
            val cross = normal.crs(mouseNormal)
            val cos = normal.dot(mouseNormal)

            if (cross < 0)
                sound[0] = ((1-cos)*100).toInt()
            if (cross > 0)
                sound[2] = ((1-cos)*100).toInt()
        }

        brain.fire(Sense.SOUND, sound)

        // at least one neuron firing per second or it will experience brain death
        if(brain.outputs.none { it.value.get() })
            brainDeadCountdown--
        else
            brainDeadCountdown += 60

        brainDeadCountdown = Math.min(brainDeadCountdown, 100)

        if(!brain.get(Action.STOP))
            forward += thrusterPower
        if(brain.get(Action.TURN_LEFT))
            torque -= torquePower
        if(brain.get(Action.TURN_RIGHT))
            torque += torquePower

//        if(brainDeadCountdown <= 0)
//            blip.kill()
    }
}
