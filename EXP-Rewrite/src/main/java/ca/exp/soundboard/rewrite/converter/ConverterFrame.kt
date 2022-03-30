package ca.exp.soundboard.rewrite.converter

import ca.exp.soundboard.rewrite.gui.SoundboardFrame
import ca.exp.soundboard.rewrite.soundboard.Utils
import net.miginfocom.swing.MigLayout
import ws.schild.jave.info.MultimediaInfo
import ws.schild.jave.progress.EncoderProgressListener
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileFilter

class ConverterFrame : JFrame() {

    private val changeOutputButton: JButton
    private val convertButton: JButton
    private val encodingMessageLabel: JLabel
    private val encodingProgressLabel: JLabel
    private val inputFileLabel: JLabel
    private val mp3RadioButton: JRadioButton
    private val outputFileLabel: JLabel
    private val wavRadioButton: JRadioButton
    private var inputFiles: Array<File>? = null
    private var outputFile: File? = null

    init {
        defaultCloseOperation = 2
        iconImage = SoundboardFrame.icon
        isResizable = false
        title = "EXP soundboard : Audio Converter"

        val buttonGroup = ButtonGroup()
        val jSeparator = JSeparator()
        val lblEncodingMessages = JLabel("Encoding Messages:")
        val lblEncodingProgress = JLabel("Encoding Progress:")
        val lblInputFile = JLabel("Input Files:")
        val lblOutputFile = JLabel("Output File:")
        val lblOutputFormat = JLabel("Output Format:")
        val selectInputButton = JButton("Select")
        val separator = JSeparator()

        changeOutputButton = JButton("Change").apply { isEnabled = false }
        convertButton = JButton("Convert").apply {
            addActionListener { convertAction() }
            isEnabled = false
        }
        encodingMessageLabel = JLabel("")
        encodingProgressLabel = JLabel("0%")
        inputFileLabel = JLabel("none selected")
        mp3RadioButton = JRadioButton("MP3").apply {
            addActionListener { renameOutputForFormat() }
            isSelected = true
        }
        outputFileLabel = JLabel("none selected")
        wavRadioButton = JRadioButton("WAV").apply { addActionListener { renameOutputForFormat() } }

        buttonGroup.add(mp3RadioButton)
        buttonGroup.add(wavRadioButton)

        selectInputButton.addActionListener {
            val fc: JFileChooser = Utils.getFileChooser()
            fc.fileFilter = null
            fc.isMultiSelectionEnabled = true
            val session: Int = fc.showDialog(null, "Select")

            if (session == 0) {
                inputFiles = fc.selectedFiles

                if (inputFiles!!.size > 1) {
                    outputFile = inputFiles!![0]
                    inputFileLabel.text = "Multiple files"
                    renameOutputForFormat()
                    outputFileLabel.text = outputFile!!.absolutePath
                } else {
                    outputFile = inputFiles!![0]
                    renameOutputForFormat()
                    inputFileLabel.text = inputFiles!![0].absolutePath
                    outputFileLabel.text = outputFile!!.absolutePath
                }

                changeOutputButton.isEnabled = true
                convertButton.isEnabled = true
            }

            fc.isMultiSelectionEnabled = false
            pack()
        }

        changeOutputButton.addActionListener {
            if ((inputFiles != null) && (outputFile != null)) {
                val fc: JFileChooser = Utils.getFileChooser()
                fc.isMultiSelectionEnabled = false

                if (inputFiles!!.size > 1) {
                    fc.fileFilter = object : FileFilter() {
                        override fun accept(f: File): Boolean {
                            return f.isDirectory
                        }

                        override fun getDescription(): String {
                            return "Folders only"
                        }
                    }

                    fc.selectedFile = outputFile
                    fc.fileSelectionMode = 1
                } else {
                    fc.fileFilter = null
                    fc.selectedFile = outputFile
                }

                val session: Int = fc.showSaveDialog(null)
                if (session == 0) {
                    outputFile = fc.selectedFile
                    println("change: " + outputFile!!.absolutePath)

                    if (inputFiles!!.size < 2) {
                        renameOutputForFormat()
                    } else {
                        outputFileLabel.text = outputFile!!.absolutePath
                    }
                }

                fc.fileSelectionMode = 0
                pack()
            }
        }

        contentPane.apply {
            layout = MigLayout(
                "", "[45px][2px][14px][2px][34px][1px][33px][222px][71px]",
                "[14px][23px][2px][14px][23px][14px][23px][2px][14px][14px]"
            )
            add(jSeparator, "cell 0 7 9 1,growx,aligny top")
            add(separator, "cell 0 2 9 1,growx,aligny top")
            add(lblInputFile, "cell 0 0 3 1,alignx left,aligny top")
            add(inputFileLabel, "cell 4 0 3 1,alignx right,aligny top")
            add(selectInputButton, "cell 0 1 3 1,alignx left,aligny top")
            add(lblOutputFile, "cell 0 3 3 1,alignx left,aligny top")
            add(outputFileLabel, "cell 4 3 3 1,alignx left,aligny top")
            add(changeOutputButton, "cell 0 4 5 1,alignx left,aligny top")
            add(lblOutputFormat, "cell 0 5 5 1,alignx left,aligny top")
            add(mp3RadioButton, "cell 0 6,alignx left,aligny top")
            add(wavRadioButton, "cell 2 6 3 1,alignx left,aligny top")
            add(convertButton, "cell 8 6,alignx left,aligny top")
            add(lblEncodingProgress, "cell 0 8 5 1,alignx right,aligny top")
            add(encodingProgressLabel, "cell 6 8,alignx left,aligny top")
            add(lblEncodingMessages, "cell 0 9 5 1,alignx right,aligny top")
            add(encodingMessageLabel, "cell 6 9,alignx left,aligny top")
        }

        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }

    private fun convertAction() {
        var cont = true

        if ((outputFile == inputFiles!![0])) {
            JOptionPane.showMessageDialog(
                null,
                "Input and output files cannot be the same",
                "File parsing error",
                JOptionPane.INFORMATION_MESSAGE
            )

            cont = false
        }

        if ((cont) && (outputFile!!.exists())) {
            val session: Int = JOptionPane.showConfirmDialog(
                null,
                "Output file already exists. Overwrite?",
                "Overwrite confirmation",
                JOptionPane.YES_NO_CANCEL_OPTION
            )

            if (session != 0) {
                cont = false
            }
        }

        if (cont) {
            if (mp3RadioButton.isSelected) {
                if (inputFiles!!.size > 1) {
                    AudioConverter.batchConvertToMP3(inputFiles, outputFile, ConvertProgressListener())
                } else {
                    AudioConverter.convertToMP3(inputFiles!![0], outputFile, ConvertProgressListener())
                }
            } else if (wavRadioButton.isSelected) {
                if (inputFiles!!.size > 1) {
                    AudioConverter.batchConvertToWAV(inputFiles, outputFile, ConvertProgressListener())
                } else {
                    AudioConverter.convertToWAV(inputFiles!![0], outputFile, ConvertProgressListener())
                }
            }

            convertButton.isEnabled = false
        }
    }

    private fun renameOutputForFormat() {
        if (inputFiles!!.size > 1) {
            if (!outputFile!!.isDirectory) {
                val inputabs: String = inputFiles!![0].absolutePath
                val slash: Int = inputabs.lastIndexOf(File.separator)
                val output: String = inputabs.substring(0, slash + 1) + "Converted"

                outputFile = File(output)
                outputFileLabel.text = outputFile!!.absolutePath
            }
        } else {
            var outputfileabs: String = outputFile!!.absolutePath
            val period: Int = outputfileabs.lastIndexOf('.')

            if (period > 0) {
                outputfileabs = outputfileabs.substring(0, period)

                if (mp3RadioButton.isSelected) {
                    outputfileabs = "$outputfileabs.mp3"
                } else if (wavRadioButton.isSelected) {
                    outputfileabs += ".wav"
                }
            }

            outputFile = File(outputfileabs)
            outputFileLabel.text = outputFile!!.absolutePath
        }
    }

    private inner class ConvertProgressListener : EncoderProgressListener {
        var current: Int = 1

        override fun message(m: String) {
            if ((inputFiles!!.size > 1) && (current < inputFiles!!.size)) {
                encodingMessageLabel.text = current.toString() + "/" + inputFiles!!.size
            }
        }

        override fun progress(p: Int) {
            val progress: Float = (p / 10).toFloat()
            encodingProgressLabel.text = "$progress%"

            if (p >= 1000) {
                if (inputFiles!!.size > 1) {
                    current += 1
                    if (current > inputFiles!!.size) {
                        encodingMessageLabel.text = "Encoding Complete!"
                        convertButton.isEnabled = true
                    }
                } else if (p == 1001) {
                    encodingMessageLabel.text = "Encoding Failed!"
                    convertButton.isEnabled = true
                } else {
                    encodingMessageLabel.text = "Encoding Complete!"
                    convertButton.isEnabled = true
                }
            }
        }

        override fun sourceInfo(m: MultimediaInfo) {}
    }

    companion object {
        private const val serialVersionUID: Long = -6720455160041920802L
    }
}
