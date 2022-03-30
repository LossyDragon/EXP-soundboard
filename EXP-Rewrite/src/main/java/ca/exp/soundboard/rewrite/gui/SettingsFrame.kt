package ca.exp.soundboard.rewrite.gui

import ca.exp.soundboard.rewrite.soundboard.MicInjector
import ca.exp.soundboard.rewrite.soundboard.Utils
import net.miginfocom.swing.MigLayout
import org.jnativehook.GlobalScreen
import org.jnativehook.keyboard.NativeKeyEvent
import org.jnativehook.keyboard.NativeKeyListener
import java.awt.Color
import java.awt.Desktop
import java.awt.Font
import java.awt.event.*
import java.net.URI
import javax.swing.*
import javax.swing.text.DefaultFormatter

class SettingsFrame private constructor() : JFrame() {

    private val decKeyInputGetter: DecKeyNativeKeyInputGetter
    private val decModSpeedHotKeyTextField: JTextField
    private val fOverlapClipsCheckbox: JCheckBox
    private val fOverlapHotkeyTextField: JTextField
    private val fOverlapKeyInputGetter: OverlapSwitchNativeKeyInputGetter
    private val incKeyInputGetter: IncKeyNativeKeyInputGetter
    private val incModSpeedHotKeyTextField: JTextField
    private val micComboBox: JComboBox<String?>
    private val modSpeedSpinner: JSpinner
    private val pttKeysInputGetter: PttKeysNativeKeyInputGetter
    private val pttKeysTextField: JTextField
    private val slowKeyInputGetter: ModSpeedKeyNativeKeyInputGetter
    private val slowKeyTextField: JTextField
    private val stopAllTextField: JTextField
    private val stopKeyInputGetter: StopKeyNativeKeyInputGetter
    private val vacComboBox: JComboBox<String?>

    init {

        contentPane.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(arg0: MouseEvent) {
                focusLostOnItems()
            }
        })

        addWindowFocusListener(object : WindowFocusListener {
            override fun windowGainedFocus(arg0: WindowEvent) {}
            override fun windowLostFocus(arg0: WindowEvent) {
                focusLostOnItems()
            }
        })

        decKeyInputGetter = DecKeyNativeKeyInputGetter()
        fOverlapKeyInputGetter = OverlapSwitchNativeKeyInputGetter()
        incKeyInputGetter = IncKeyNativeKeyInputGetter()
        pttKeysInputGetter = PttKeysNativeKeyInputGetter()
        slowKeyInputGetter = ModSpeedKeyNativeKeyInputGetter()
        stopKeyInputGetter = StopKeyNativeKeyInputGetter()

        isResizable = false
        defaultCloseOperation = 2
        title = "Settings"

        stopAllTextField = JTextField().apply {
            columns = 10
            isEditable = false
            text = NativeKeyEvent.getKeyText(Utils.stopKey)
            addFocusListener(object : FocusAdapter() {
                override fun focusLost(arg0: FocusEvent) {
                    // GlobalScreen.getInstance().removeNativeKeyListener(SettingsFrame.this.stopKeyInputGetter);
                    GlobalScreen.removeNativeKeyListener(stopKeyInputGetter)
                    background = Color.WHITE
                }
            })
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(arg0: MouseEvent) {
                    background = Color.CYAN
                    GlobalScreen.addNativeKeyListener(stopKeyInputGetter)
                }
            })
        }

        slowKeyTextField = JTextField().apply {
            columns = 10
            isEditable = false
            text = NativeKeyEvent.getKeyText(Utils.modifiedSpeedKey)
            addFocusListener(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent) {
                    GlobalScreen.removeNativeKeyListener(slowKeyInputGetter)
                    background = Color.WHITE
                }
            })
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(arg0: MouseEvent) {
                    background = Color.CYAN
                    GlobalScreen.addNativeKeyListener(slowKeyInputGetter)
                }
            })
        }

        incModSpeedHotKeyTextField = JTextField().apply {
            columns = 10
            isEditable = false
            text = NativeKeyEvent.getKeyText(Utils.modspeedupKey)
            addFocusListener(object : FocusAdapter() {
                override fun focusLost(arg0: FocusEvent) {
                    GlobalScreen.removeNativeKeyListener(incKeyInputGetter)
                    background = Color.WHITE
                }
            })
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(arg0: MouseEvent) {
                    GlobalScreen.addNativeKeyListener(incKeyInputGetter)
                    background = Color.CYAN
                }
            })
        }

        decModSpeedHotKeyTextField = JTextField().apply {
            columns = 10
            isEditable = false
            text = NativeKeyEvent.getKeyText(Utils.modspeeddownKey)
            addFocusListener(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent) {
                    background = Color.WHITE
                    GlobalScreen.removeNativeKeyListener(decKeyInputGetter)
                }
            })
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    GlobalScreen.addNativeKeyListener(decKeyInputGetter)
                    background = Color.CYAN
                }
            })
        }

        pttKeysTextField = JTextField().apply {
            columns = 10
            focusTraversalKeysEnabled = false
            isEditable = false
            addFocusListener(object : FocusAdapter() {
                override fun focusLost(arg0: FocusEvent) {
                    background = Color.WHITE
                    removeKeyListener(pttKeysInputGetter)
                }
            })
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(arg0: MouseEvent) {
                    removeKeyListener(pttKeysInputGetter)
                    addKeyListener(pttKeysInputGetter)
                    background = Color.CYAN
                }
            })
        }

        fOverlapHotkeyTextField = JTextField().apply {
            columns = 10
            isEditable = false
            text = NativeKeyEvent.getKeyText(Utils.overlapSwitchKey)
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    GlobalScreen.addNativeKeyListener(fOverlapKeyInputGetter)
                    background = Color.CYAN
                }
            })
            addFocusListener(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent) {
                    background = Color.WHITE
                    GlobalScreen.removeNativeKeyListener(fOverlapKeyInputGetter)
                }
            })
        }

        fOverlapClipsCheckbox = JCheckBox("").apply {
            isSelected = Utils.isOverlapSameClipWhilePlaying()
            addActionListener {
                val selected = isSelected
                Utils.overlapSameClipWhilePlaying = selected
            }
        }

        modSpeedSpinner = JSpinner().apply {
            model = SpinnerNumberModel(Utils.getModifiedPlaybackSpeed(), 0.1f, 2.0f, 0.05f)
        }.also {
            val comp = it.editor
            val field = comp.getComponent(0) as JFormattedTextField
            field.isEditable = false
            val formatter = field.formatter as DefaultFormatter
            formatter.commitsOnValidEdit = true
        }

        modSpeedSpinner.addChangeListener {
            val speed = (modSpeedSpinner.value as Float).toFloat()
            if (speed in 0.1f..2.0f) {
                Utils.setModifiedPlaybackSpeed(speed)
            }
        }

        val btnProjectWebsite = JButton("Project Website").apply {
            addActionListener {
                Desktop.getDesktop().browse(URI("https://sourceforge.net/projects/expsoundboard/"))
            }
        }
        val lblExpenosa = JLabel(" © Expenosa. 2014.")
        val lblMicInjectorSettings = JLabel("Mic Injector settings:").apply { foreground = Color.RED }
        val lblMicrophone = JLabel("Microphone:")
        val lblModifiedPlaybackSpeed = JLabel("Modified playback speed multiplier:")
        val lblModifierSpeedIncrement = JLabel("Modifier speed Increment hotkey:")
        val lblNewLabel = JLabel("Modifier speed Decrement hotkey:")
        val lblOverlapSameSound = JLabel("Overlap same sound file:").apply { font = Font("Tahoma", 1, 11) }
        val lblOverlapSwitchHotkey = JLabel("Overlap switch hotkey:")
        val lblUseMicInjector =
            JLabel("*Use Mic Injector when your using a virtual audio cable as your input on other software.").apply {
                font = Font("Tahoma", 2, 10)
            }
        val lblVersion = JLabel("Version: 0.5")
        val lblVirtualAudioCable = JLabel("Virtual Audio Cable:")
        val lblhalfSpeedPlayback = JLabel("'Modified playback speed' combo key:")
        val lblpushToTalk = JLabel("VoIP 'Push To Talk' Key(s): ").apply { font = Font("Tahoma", 1, 11) }
        val lblstopAllSounds = JLabel("'Stop All Sounds' hotkey:").apply { font = Font("Tahoma", 1, 11) }
        val separator = JSeparator().apply { foreground = Color.BLACK }

        micComboBox = JComboBox()
        vacComboBox = JComboBox()
        iconImage = SoundboardFrame.icon
        pttKeysInputGetter.updateTextField()

        var inputArray: Array<String?>
        val inputMixers = MicInjector.getMixerNames(MicInjector.targetDataLineInfo)
        val inputs = inputMixers.also { inputArray = it }.size
        for (i in 0 until inputs) {
            val input = inputArray[i]
            micComboBox.addItem(input)
        }
        micComboBox.selectedItem = SoundboardFrame.micInjectorInputMixerName
        micComboBox.addItemListener { e ->
            if (e.stateChange == 1) {
                updateMicInjectorSettings()
            }
        }

        var outputArray: Array<String?>
        val outputMixers = MicInjector.getMixerNames(MicInjector.sourceDataLineInfo)
        val outputs = outputMixers.also { outputArray = it }.size
        for (i in 0 until outputs) {
            val output = outputArray[i]
            vacComboBox.addItem(output)
        }
        vacComboBox.selectedItem = SoundboardFrame.micInjectorOutputMixerName
        vacComboBox.addItemListener { e ->
            if (e.stateChange == 1) {
                updateMicInjectorSettings()
            }
        }

        contentPane.layout = MigLayout(
            "", "[101px][20px][45px][13px][71px][4px][34px,grow][10px][135px]",
            "[20px][20px][20px][20px][20px][20px][21px][][2px][14px][20px][20px][13px][2px][14px][23px]"
        )
        contentPane.add(lblstopAllSounds, "cell 0 0 3 1,alignx left,aligny center")
        contentPane.add(lblhalfSpeedPlayback, "cell 0 1 5 1,growx,aligny center")
        contentPane.add(lblModifiedPlaybackSpeed, "cell 0 2 3 1,alignx left,aligny center")
        contentPane.add(stopAllTextField, "cell 6 0 3 1,growx,aligny top")
        contentPane.add(slowKeyTextField, "cell 6 1 3 1,growx,aligny top")
        contentPane.add(modSpeedSpinner, "cell 6 2 3 1,growx,aligny top")
        contentPane.add(lblOverlapSwitchHotkey, "cell 0 7 3 1")
        contentPane.add(fOverlapHotkeyTextField, "cell 6 7 3 1,growx")
        contentPane.add(separator, "cell 0 13 9 1,growx,aligny top")
        contentPane.add(btnProjectWebsite, "cell 8 15,alignx right,aligny top")
        contentPane.add(lblVersion, "cell 0 14,alignx left,aligny top")
        contentPane.add(lblExpenosa, "cell 8 14,alignx right,aligny top")
        contentPane.add(lblMicInjectorSettings, "cell 0 9,alignx left,aligny top")
        contentPane.add(lblMicrophone, "cell 0 10,alignx left,aligny center")
        contentPane.add(lblVirtualAudioCable, "cell 0 11,alignx left,aligny center")
        contentPane.add(vacComboBox, "cell 2 11 7 1,growx,aligny top")
        contentPane.add(micComboBox, "cell 2 10 7 1,growx,aligny top")
        contentPane.add(lblUseMicInjector, "cell 0 12 9 1,alignx left,aligny top")
        contentPane.add(separator, "cell 0 8 9 1,growx,aligny top")
        contentPane.add(lblNewLabel, "cell 0 4 5 1,growx,aligny center")
        contentPane.add(lblModifierSpeedIncrement, "cell 0 3 5 1,growx,aligny center")
        contentPane.add(lblpushToTalk, "cell 0 5 3 1,alignx left,aligny center")
        contentPane.add(lblOverlapSameSound, "cell 0 6 3 1,alignx left,growy")
        contentPane.add(fOverlapClipsCheckbox, "cell 6 6,alignx left,aligny top")
        contentPane.add(pttKeysTextField, "cell 6 5 3 1,growx,aligny top")
        contentPane.add(decModSpeedHotKeyTextField, "cell 6 4 3 1,growx,aligny top")
        contentPane.add(incModSpeedHotKeyTextField, "cell 6 3 3 1,growx,aligny top")
        pack()

        isVisible = true
    }

    private fun updateMicInjectorSettings() {
        SoundboardFrame.micInjectorInputMixerName = (micComboBox.selectedItem as String)
        SoundboardFrame.micInjectorOutputMixerName = (vacComboBox.selectedItem as String)

        if (SoundboardFrame.useMicInjector) {
            Utils.startMicInjector(
                SoundboardFrame.micInjectorInputMixerName,
                SoundboardFrame.micInjectorOutputMixerName
            )
        }
    }

    fun updateDisplayedModSpeed() {
        modSpeedSpinner.value =
            java.lang.Float.valueOf(Utils.getModifiedPlaybackSpeed())
    }

    fun updateOverlapSwitchCheckBox() {
        fOverlapClipsCheckbox.isSelected = Utils.isOverlapSameClipWhilePlaying()
    }

    override fun dispose() {
        super.dispose()
        GlobalScreen.removeNativeKeyListener(slowKeyInputGetter)
        GlobalScreen.removeNativeKeyListener(stopKeyInputGetter)
        GlobalScreen.removeNativeKeyListener(incKeyInputGetter)
        GlobalScreen.removeNativeKeyListener(decKeyInputGetter)

        pttKeysTextField.removeKeyListener(pttKeysInputGetter)

        instance = null
    }

    private fun focusLostOnItems() {
        decModSpeedHotKeyTextField.background = Color.WHITE
        fOverlapHotkeyTextField.background = Color.WHITE
        incModSpeedHotKeyTextField.background = Color.WHITE
        pttKeysTextField.background = Color.WHITE
        slowKeyTextField.background = Color.WHITE
        stopAllTextField.background = Color.WHITE

        pttKeysInputGetter.clearPressedKeys()

        GlobalScreen.removeNativeKeyListener(stopKeyInputGetter)
        GlobalScreen.removeNativeKeyListener(slowKeyInputGetter)
        GlobalScreen.removeNativeKeyListener(incKeyInputGetter)
        GlobalScreen.removeNativeKeyListener(decKeyInputGetter)
        GlobalScreen.removeNativeKeyListener(fOverlapKeyInputGetter)

        pttKeysTextField.removeKeyListener(pttKeysInputGetter)
    }

    private inner class OverlapSwitchNativeKeyInputGetter : NativeKeyListener {
        var key = Utils.stopKey

        private fun updateTextField() {
            val keyname = NativeKeyEvent.getKeyText(key)
            fOverlapHotkeyTextField.text = keyname
        }

        override fun nativeKeyPressed(e: NativeKeyEvent) {
            key = e.keyCode
            Utils.overlapSwitchKey = key
            updateTextField()
        }

        override fun nativeKeyReleased(arg0: NativeKeyEvent) {}

        override fun nativeKeyTyped(arg0: NativeKeyEvent) {}
    }

    private inner class PttKeysNativeKeyInputGetter : KeyListener {
        var pressedkeys: MutableList<Int> = mutableListOf()

        override fun keyPressed(e: KeyEvent) {
            val key = e.keyCode
            pressedkeys.add(Integer.valueOf(key))

            Utils.pTTkeys = pressedkeys
            updateTextField()

            println("PPT listener key pressed: " + KeyEvent.getKeyText(key))
        }

        override fun keyReleased(e: KeyEvent) {
            val key = Integer.valueOf(e.keyCode)
            pressedkeys.remove(key)

            println("PPT listener key released: " + KeyEvent.getKeyText(key.toInt()))
        }

        override fun keyTyped(arg0: KeyEvent) {}

        @Synchronized
        fun updateTextField() {
            val keyString = StringBuilder()
            val keys = Utils.pTTkeys

            for (i in keys.indices) {
                if (i == 0) {
                    keyString.append(KeyEvent.getKeyText(keys[i]))
                } else {
                    keyString.append(" + " + KeyEvent.getKeyText(keys[i]))
                }
            }

            pttKeysTextField.text = keyString.toString()
            println("PTT listener text field updated")
        }

        @Synchronized
        fun clearPressedKeys() {
            pressedkeys.clear()
            println("PTT listener keys cleared")
        }
    }

    private inner class ModSpeedKeyNativeKeyInputGetter : NativeKeyListener {
        var key = Utils.modifiedSpeedKey

        override fun nativeKeyPressed(e: NativeKeyEvent) {
            key = e.keyCode
            Utils.modifiedSpeedKey = key
            updateTextField()
        }

        override fun nativeKeyReleased(e: NativeKeyEvent) {}

        override fun nativeKeyTyped(arg0: NativeKeyEvent) {}

        @Synchronized
        private fun updateTextField() {
            val keyname = NativeKeyEvent.getKeyText(key)
            slowKeyTextField.text = keyname
        }
    }

    private inner class StopKeyNativeKeyInputGetter : NativeKeyListener {
        var key = Utils.stopKey

        override fun nativeKeyPressed(e: NativeKeyEvent) {
            key = e.keyCode
            Utils.stopKey = key
            updateTextField()
        }

        override fun nativeKeyReleased(e: NativeKeyEvent) {}

        override fun nativeKeyTyped(arg0: NativeKeyEvent) {}

        @Synchronized
        private fun updateTextField() {
            val keyname = NativeKeyEvent.getKeyText(key)
            stopAllTextField.text = keyname
        }
    }

    private inner class IncKeyNativeKeyInputGetter : NativeKeyListener {
        var key = Utils.modspeedupKey
        override fun nativeKeyPressed(e: NativeKeyEvent) {

            key = e.keyCode
            Utils.modspeedupKey = key
            updateTextField()
        }

        override fun nativeKeyReleased(e: NativeKeyEvent) {}

        override fun nativeKeyTyped(arg0: NativeKeyEvent) {}

        @Synchronized
        private fun updateTextField() {
            val keyname = NativeKeyEvent.getKeyText(key)
            incModSpeedHotKeyTextField.text = keyname
        }
    }

    private inner class DecKeyNativeKeyInputGetter : NativeKeyListener {
        var key = Utils.modspeeddownKey

        override fun nativeKeyPressed(e: NativeKeyEvent) {
            key = e.keyCode
            Utils.modspeeddownKey = key
            updateTextField()
        }

        override fun nativeKeyReleased(e: NativeKeyEvent) {}

        override fun nativeKeyTyped(arg0: NativeKeyEvent) {}

        @Synchronized
        private fun updateTextField() {
            val keyname = NativeKeyEvent.getKeyText(key)
            decModSpeedHotKeyTextField.text = keyname
        }
    }

    companion object {
        private const val serialVersionUID = -4758092886690912749L

        @JvmField
        var instance: SettingsFrame? = null

        val instanceOf: SettingsFrame
            get() {
                if (instance == null) {
                    instance = SettingsFrame()
                } else {
                    instance!!.isVisible = true
                    instance!!.requestFocus()
                }
                return instance!!
            }
    }
}
