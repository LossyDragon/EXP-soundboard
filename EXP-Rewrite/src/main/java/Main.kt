import ca.exp.soundboard.rewrite.gui.SoundboardFrame
import ca.exp.soundboard.rewrite.soundboard.Utils
import org.jnativehook.GlobalScreen
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger

@Suppress("UNUSED_PARAMETER")
fun main(args: Array<String>) {

    // https://stackoverflow.com/a/30562956/13225929
    LogManager.getLogManager().reset()
    val logger = Logger.getLogger(GlobalScreen::class.java.getPackage().name)
    logger.level = Level.OFF

    Utils.initGlobalKeyLibrary()

    val soundboard = SoundboardFrame()
    soundboard.isVisible = true
}
