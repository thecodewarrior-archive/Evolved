package thecodewarrior.evolved

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer




class EvolvedGame(val WIDTH: Int, val HEIGHT: Int) : InputProcessor {

    companion object {
        val tps = 60f
        val ppm = 32f // pixels per meter
        var zoom = 1f
        var curZoom = 0f
    }

    internal val world = EvolveWorld(this)
    internal val debug = Box2DDebugRenderer()
    internal val camera = OrthographicCamera()

    internal var spriteBatch = SpriteBatch()
    internal var shapeRenderer = ShapeRenderer()
    internal var tickTime = 0f

    init {
        EvolveWorld.mainWorld = world

        updateZoom()
        zoom = 0.5f
    }

    fun updateZoom() {
        if(zoom == curZoom)
            return
        curZoom = zoom
        camera.setToOrtho(false, 1f/zoom * Gdx.graphics.width / ppm, 1f/zoom * Gdx.graphics.height / ppm)
        camera.translate(vec(1f/zoom * -Gdx.graphics.width/(2f*ppm), 1f/zoom * -Gdx.graphics.height/(2f*ppm)))
    }

    fun render() {
        updateZoom()
        camera.update()

        val x1 = Gdx.input.x
        val y1 = Gdx.input.y
        val input = Vector3(x1.toFloat(), y1.toFloat(), 0f)
        camera.unproject(input)

        world.updateMouse(input.x, input.y)


        spriteBatch.projectionMatrix = camera.combined

        tickTime -= Gdx.graphics.deltaTime
        if(tickTime <= 0) {
            tickTime = 1/tps
            world.tick()
        }
        Gdx.gl.glClearColor(1f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        world.draw(shapeRenderer)
        shapeRenderer.end()

        spriteBatch.projectionMatrix = camera.combined
        spriteBatch.begin()
        world.draw(spriteBatch)
        spriteBatch.end()

        world.draw(camera.combined)

        debug.render(world.physWorld, camera.combined)
    }

    fun dispose() {
        shapeRenderer.dispose()
        spriteBatch.dispose()
        world.dispose()
    }

    // input

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val x1 = Gdx.input.x
        val y1 = Gdx.input.y
        val input = Vector3(x1.toFloat(), y1.toFloat(), 0f)
        camera.unproject(input)

        return world.click(input.x, input.y)
    }

    override fun scrolled(amount: Int): Boolean {
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }

    override fun keyDown(keycode: Int): Boolean {
        return false
    }
}

class EvolvedGameAdapter(val WIDTH: Int, val HEIGHT: Int) : ApplicationAdapter() {
    private lateinit var game: EvolvedGame

    override fun create() {
        game = EvolvedGame(WIDTH, HEIGHT)
        Gdx.input.inputProcessor = game
    }

    override fun render() {
        game.render()
    }

    override fun dispose() {
        game.dispose()
    }


}
