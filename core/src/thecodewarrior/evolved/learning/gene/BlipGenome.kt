package thecodewarrior.evolved.learning.gene

import thecodewarrior.evolved.learning.network.BrainData

/**
 * Created by TheCodeWarrior
 */
class BlipGenome : Genome() {
    init {
        genes[BlipGenes.BRAIN] = BrainData.random()
    }
}

enum class BlipGenes {
    BRAIN
}
