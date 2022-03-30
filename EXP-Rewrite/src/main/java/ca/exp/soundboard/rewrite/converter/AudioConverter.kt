package ca.exp.soundboard.rewrite.converter

import ws.schild.jave.Encoder
import ws.schild.jave.EncoderException
import ws.schild.jave.MultimediaObject
import ws.schild.jave.encode.AudioAttributes
import ws.schild.jave.encode.EncodingAttributes
import ws.schild.jave.progress.EncoderProgressListener
import java.io.File
import java.lang.IllegalArgumentException
import java.net.MalformedURLException
import java.util.ArrayList
import javax.swing.JOptionPane
import kotlin.Throws

object AudioConverter {

    private const val channels = 2
    private const val formatMp3 = "mp3"
    private const val formatWav = "wav"
    private const val mp3 = "libmp3lame"
    private const val mp3bitrate = 256000
    private const val sampleRate = 44100
    private const val wav = "pcm_s16le"

    fun batchConvertToMP3(inputFiles: Array<File>?, outputFolder: File?, listener: EncoderProgressListener?) {
        Thread {
            var arrayOfFile: Array<File>
            val j = (inputFiles.also { arrayOfFile = (it)!! })!!.size

            for (i in 0 until j) {
                try {
                    val input = arrayOfFile[i]
                    val output = getAbsoluteForOutputExtensionAndFolder(input, outputFolder, ".mp3")

                    println("processing: " + output.absolutePath)

                    mp3(input, output, listener)
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    fun batchConvertToWAV(inputFiles: Array<File>?, outputFolder: File?, listener: EncoderProgressListener?) {
        Thread {
            var arrayOfFile: Array<File>
            val j: Int = (inputFiles.also { arrayOfFile = (it)!! })!!.size

            for (i in 0 until j) {
                try {
                    val input: File = arrayOfFile[i]
                    val output: File = getAbsoluteForOutputExtensionAndFolder(input, outputFolder, ".wav")

                    println("processing: " + output.absolutePath)

                    wav(input, output, listener)
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    fun convertToMP3(inputFile: File, outputFile: File?, listener: EncoderProgressListener?) {
        Thread {
            try {
                mp3(inputFile, outputFile, listener)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
        }.start()
    }

    fun convertToWAV(inputFile: File, outputFile: File?, listener: EncoderProgressListener?) {
        Thread {
            try {
                wav(inputFile, outputFile, listener)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
        }.start()
    }

    @Throws(MalformedURLException::class)
    private fun mp3(inputFile: File, outputFile: File?, listener: EncoderProgressListener?) {
        val multis = ArrayList<MultimediaObject>()
        val audio = AudioAttributes().apply {
            setCodec(mp3)
            setBitRate(mp3bitrate)
            setChannels(AudioConverter.channels)
            setSamplingRate(sampleRate)
        }
        val ea = EncodingAttributes().apply {
            setOutputFormat(formatMp3)
            setAudioAttributes(audio)
        }

        multis.add(MultimediaObject(inputFile.toURI().toURL()))

        try {
            val encoder = Encoder()
            if (listener != null) {
                encoder.encode(multis, outputFile, ea, listener)
            } else {
                encoder.encode(multis, outputFile, ea)
            }
        } catch (e: IllegalArgumentException) {
            JOptionPane.showMessageDialog(
                null,
                "Input file formatting/encoding is incompatible\n" + inputFile.name,
                "Input File incompatible",
                JOptionPane.ERROR_MESSAGE
            )

            listener!!.progress(1001)
            e.printStackTrace()
        } catch (e: EncoderException) {
            JOptionPane.showMessageDialog(
                null,
                "Input file formatting/encoding is incompatible\n" + inputFile.name,
                "Input File incompatible",
                JOptionPane.ERROR_MESSAGE
            )

            listener!!.progress(1001)
            e.printStackTrace()
        }
    }

    @Throws(MalformedURLException::class)
    private fun wav(inputFile: File, outputFile: File?, listener: EncoderProgressListener?) {
        val multis = ArrayList<MultimediaObject>()
        val audio = AudioAttributes().apply {
            setCodec(wav)
        }
        val ea = EncodingAttributes().apply {
            setOutputFormat(formatWav)
            setAudioAttributes(audio)
        }

        multis.add(MultimediaObject(inputFile.toURI().toURL()))

        try {
            val encoder = Encoder()
            if (listener != null) {
                encoder.encode(multis, outputFile, ea, listener)
            } else {
                encoder.encode(multis, outputFile, ea)
            }
        } catch (e: IllegalArgumentException) {
            JOptionPane.showMessageDialog(
                null,
                "Input file formatting/encoding is incompatible\n" + inputFile.name,
                "Input File incompatible",
                JOptionPane.ERROR_MESSAGE
            )

            listener!!.progress(1001)
            e.printStackTrace()
        } catch (e: EncoderException) {
            JOptionPane.showMessageDialog(
                null,
                "Input file formatting/encoding is incompatible\n" + inputFile.name,
                "Input File incompatible",
                JOptionPane.ERROR_MESSAGE
            )

            listener!!.progress(1001)
            e.printStackTrace()
        }
    }

    private fun getAbsoluteForOutputExtensionAndFolder(inputFile: File, outputFolder: File?, doText: String): File {
        var filename = inputFile.name
        val period = filename.lastIndexOf('.')

        if (period > 0) {
            filename = filename.substring(0, period) + doText
        }

        return File(outputFolder.toString() + File.separator + filename)
    }
}
