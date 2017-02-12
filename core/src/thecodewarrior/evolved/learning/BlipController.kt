package thecodewarrior.evolved.learning

import thecodewarrior.evolved.clamp
import thecodewarrior.evolved.gcm
import thecodewarrior.evolved.learning.genetics.BlipGenome

/**
 * Created by TheCodeWarrior
 */
class BlipController(val blip: EntityBlip, val genome: BlipGenome) {
    val thrusterPower = 0.5f
    val torquePower = 0.2f

    var forward = 0f
        private set

    var torque = 0f
        private set

    var breedWillingness = 0.0
    var breedCooldown = 500
    var vision = listOf<DeepColor>()
    val realVision = mutableMapOf<DeepChannel, DoubleArray>()

    init {
        if(genome.brain.nets.size == 0) // brain dead
            blip.kill()
    }

    fun raytrace(): List<DeepColor> {
        val list = mutableListOf<DeepColor>()

        val interval = 6f
        var angle = (genome.mainData.fov*interval).toInt() / interval

        var a = 0f
        while(a <= angle) {

            var end = blip.normal().rotate(a-angle/2).scl(500f)
            var color = DeepColor()
            var minDist = Float.POSITIVE_INFINITY
            blip.physWorld.rayCast(cast@{ f, point, normal, d ->
                if(f.body.userData != this.blip && f.body.userData is Visible) {
                    if(d < minDist) {
                        color = (f.body.userData as Visible).color
                        minDist = d
                    }
                }
                return@cast -1f
            }, blip.pos, end)

            list.add(color)

            a += interval
        }

        return list
    }

    fun channel(list: List<DeepColor>, channel: DeepChannel, width: Int): DoubleArray {

        if(list.size == 0)
            return doubleArrayOf()
        val gcm = gcm(list.size, width)

        val want = width/gcm
        val have = list.size/gcm

        return downsample(upsample(list.map { it[channel] }.toDoubleArray(), want), have)
    }

    fun upsample(array: DoubleArray, factor: Int): DoubleArray {
        if(factor == 1)
            return array
        val newArr = DoubleArray(array.size*factor)
        var i = 0
        array.forEach { value ->
            for(j in 1..factor) {
                newArr[i++] = value
            }
        }
        return newArr
    }

    fun downsample(array: DoubleArray, factor: Int): DoubleArray {
        if(factor == 1)
            return array
        val newArr = DoubleArray(array.size/factor)
        var i = 0
        var loop = 0
        var running = 0.0
        array.forEach { value ->
            if(loop == factor) {
                newArr[i++] = running/factor
                running = 0.0
                loop = 0
            }
            running += value
            loop++
        }

        return newArr
    }


    fun tick() {
        breedCooldown--

        vision = raytrace().reversed() // I'm lazy and the array is going right -> left, it should be left -> right 'cause english writing and stuff
        realVision.clear()

        DeepChannel.values().forEach { channel ->
            val arr = channel(vision, channel, genome.mainData.visionResolution)
            realVision[channel] = arr
            genome.brain[EnumSense.byChannel[channel]!!] = arr
        }

        genome.brain[EnumSense.ENERGY] = doubleArrayOf(blip.food / blip.maxFood.toDouble())

        genome.brain.update()

        forward = genome.brain[EnumAction.FORWARD].toFloat() * thrusterPower
        torque = (genome.brain[EnumAction.LEFT] - genome.brain[EnumAction.RIGHT]).toFloat() * torquePower

        breedWillingness = genome.brain[EnumAction.BREED].clamp(-1.0, 1.0)

        if(blip.age > genome.mainData.maxAge)
            blip.kill()
    }
}
