package ca.exp.soundboard.rewrite.gui

import ca.exp.soundboard.rewrite.soundboard.Soundboard
import ca.exp.soundboard.rewrite.soundboard.SoundboardEntry
import ca.exp.soundboard.rewrite.soundboard.Utils
import org.jnativehook.GlobalScreen
import org.jnativehook.keyboard.NativeKeyEvent
import org.jnativehook.keyboard.NativeKeyListener
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.filechooser.FileFilter

class SoundboardEntryEditor(
    private var soundboardframe: SoundboardFrame
) : JFrame() {

    lateinit var keyNums: IntArray
    private val inputGetter: NativeKeyInputGetter
    private val keysTextField: JTextField
    private val selectedSoundClipLabel: JLabel
    private var soundboard: Soundboard = SoundboardFrame.soundboard
    private var soundboardEntry: SoundboardEntry? = null
    private var soundfile: File? = null

    constructor(soundboardFrame: SoundboardFrame, entry: SoundboardEntry) : this(soundboardFrame) {
        soundfile = File(entry.fileString)

        keyNums = entry.activationKeysNumbers
        selectedSoundClipLabel.text = entry.fileString
        keysTextField.text = entry.activationKeysAsReadableString

        pack()
    }

    init {
        inputGetter = NativeKeyInputGetter()
        selectedSoundClipLabel = JLabel("None selected")
        defaultCloseOperation = DISPOSE_ON_CLOSE
        iconImage = SoundboardFrame.icon
        title = "SoundboardStage Entry Editor"

        val btnDone = JButton("Done").apply { addActionListener { submit() } }
        val btnSelect = JButton("Select")
        val groupLayout = GroupLayout(contentPane)
        val lblMacroKeys = JLabel("HotKeys:")
        val lblRightclickTo = JLabel("* Right-click to clear hotkeys")
        val lblSoundClip = JLabel("Sound clip:")
        val jSeparator = JSeparator()

        btnSelect.addActionListener {
            val filechooser = Utils.getFileChooser().apply {
                isMultiSelectionEnabled = true
                fileFilter = AudioClipFileFilter()
            }

            val session = filechooser.showDialog(null, "Select")
            if (session == 0) {
                val selected = filechooser.selectedFiles
                if (selected.size > 1) {
                    multiAdd(selected)
                } else {
                    soundfile = selected[0]
                }

                filechooser.isMultiSelectionEnabled = false

                if (Utils.isFileSupported(soundfile)) {
                    selectedSoundClipLabel.text = soundfile!!.absolutePath
                } else {
                    soundfile = null
                    JOptionPane.showMessageDialog(
                        null,
                        "${soundfile!!.name} uses an unsupported codec format.",
                        "Unsupported Format",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }

            filechooser.isMultiSelectionEnabled = false
            pack()
        }

        keysTextField = JTextField().apply {
            text = "none"
            isEditable = false
            columns = 10
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    if (e.button == 1) {
                        background = Color.CYAN
                        GlobalScreen.addNativeKeyListener(inputGetter)
                        inputGetter.clearPressedKeys()
                    } else if (e.button == 3) {
                        background = Color.WHITE
                        GlobalScreen.removeNativeKeyListener(inputGetter)
                        inputGetter.clearPressedKeys()
                        keyNums = IntArray(0)
                        text = "none"
                    }
                }
            })
        }

        groupLayout.setHorizontalGroup(
            groupLayout
                .createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    groupLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                            groupLayout
                                .createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(jSeparator, -1, 414, 32767)
                                .addComponent(this.selectedSoundClipLabel, -1, 414, 32767)
                                .addGroup(
                                    groupLayout
                                        .createSequentialGroup()
                                        .addComponent(lblSoundClip)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnSelect)
                                )
                                .addComponent(lblMacroKeys)
                                .addComponent(this.keysTextField, -1, 414, 32767)
                                .addGroup(
                                    GroupLayout.Alignment.TRAILING,
                                    groupLayout
                                        .createSequentialGroup()
                                        .addComponent(lblRightclickTo)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 311, 32767)
                                        .addComponent(btnDone)
                                )
                        )
                        .addContainerGap()
                )
        )

        groupLayout
            .setVerticalGroup(
                groupLayout
                    .createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(
                        groupLayout
                            .createSequentialGroup()
                            .addContainerGap()
                            .addGroup(
                                groupLayout
                                    .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                    .addComponent(lblSoundClip)
                                    .addComponent(btnSelect)
                            )
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(this.selectedSoundClipLabel).addGap(13)
                            .addComponent(jSeparator, -2, -1, -2)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(lblMacroKeys)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(this.keysTextField, -2, -1, -2)
                            .addGap(19)
                            .addGroup(
                                groupLayout
                                    .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                    .addComponent(btnDone)
                                    .addComponent(lblRightclickTo)
                            )
                            .addContainerGap(-1, 32767)
                    )
            )

        contentPane.layout = groupLayout
        contentPane.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(arg0: MouseEvent) {
                keysTextField.background = Color.WHITE
                GlobalScreen.removeNativeKeyListener(inputGetter)
            }
        })

        pack()
        setLocationRelativeTo(soundboardframe)
        isVisible = true
    }

    private fun submit() {
        if (soundfile == null)
            return

        if (soundboardEntry == null) {
            soundboard.addEntry(soundfile, keyNums)
            soundboardframe.updateSoundboardTable()
        } else {
            soundboardEntry!!.setFile(soundfile)
            soundboardEntry!!.activationKeys = keyNums
            soundboardframe.updateSoundboardTable()
        }

        dispose()
    }

    private fun multiAdd(files: Array<File>) {
        var arrayOfFile: Array<File>
        val j = files.also { arrayOfFile = it }.size

        for (i in 0 until j) {
            val file = arrayOfFile[i]
            soundboard.addEntry(file, null)
        }

        soundboardframe.updateSoundboardTable()

        dispose()
    }

    override fun dispose() {
        super.dispose()
        GlobalScreen.removeNativeKeyListener(inputGetter)
    }

    private inner class NativeKeyInputGetter : NativeKeyListener {
        var pressedKeys = 0
        var pressedKeyNums = ArrayList<Int>()
        var pressedKeyNames = ArrayList<String>()

        override fun nativeKeyPressed(e: NativeKeyEvent) {
            if (pressedKeys <= 0) {
                pressedKeyNames.clear()
                pressedKeyNums.clear()
            }

            pressedKeys += 1

            val key = e.keyCode
            val keyname = NativeKeyEvent.getKeyText(key)

            println("key pressed: $key $keyname")

            for (i in pressedKeyNums) {
                if (i == key) {
                    return
                }
            }

            pressedKeyNums.add(key)
            pressedKeyNames.add(keyname)

            updateTextField()

            val macroKeys = IntArray(pressedKeyNums.size)
            for (i in macroKeys.indices) {
                macroKeys[i] = pressedKeyNums[i]
            }

            keyNums = macroKeys
        }

        override fun nativeKeyReleased(e: NativeKeyEvent) {
            pressedKeys -= 1
            if (pressedKeys < 0) {
                pressedKeys = 0
            }
            val key = e.keyCode
            pressedKeyNums.remove(Integer.valueOf(key))
            pressedKeyNames.remove(NativeKeyEvent.getKeyText(key))
        }

        override fun nativeKeyTyped(arg0: NativeKeyEvent) {}
        fun clearPressedKeys() {
            pressedKeys = 0
            pressedKeyNames.clear()
            pressedKeyNums.clear()
        }

        @Synchronized
        private fun updateTextField() {
            var allKeys = ""
            for (key in pressedKeyNames) {
                allKeys = "$allKeys$key+"
            }
            allKeys = allKeys.substring(0, allKeys.length - 1)
            keysTextField.text = allKeys
        }
    }

    private class AudioClipFileFilter : FileFilter() {
        override fun accept(file: File): Boolean {
            if (file.isDirectory) {
                return true
            }

            val filename = file.name.lowercase(Locale.getDefault())
            return filename.endsWith(".wav") || filename.endsWith(".mp3")
        }

        override fun getDescription(): String {
            return ".mp3 or uncompressed .wav"
        }
    }

    companion object {
        private const val serialVersionUID = -8420285054567246768L
    }
}