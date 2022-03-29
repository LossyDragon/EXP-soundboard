package ca.exp.soundboard.rewrite.converter;


import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.progress.EncoderProgressListener;

import javax.swing.*;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;

public class AudioConverter {
    private static final String mp3 = "libmp3lame";
    private static final String wav = "pcm_s16le";
    private static final Integer mp3bitrate = new Integer(256000);
    private static final Integer channels = new Integer(2);
    private static final Integer samplerate = new Integer(44100);

    public static void batchConvertToMP3(File[] inputfiles, final File outputfolder,
                                         final EncoderProgressListener listener) {
        new Thread(new Runnable() {
            public void run() {
                File[] arrayOfFile;
                // int j = (arrayOfFile = AudioConverter.this).length;
                int j = (arrayOfFile = inputfiles).length;
                for (int i = 0; i < j; i++) {
                    try {
                        File input = arrayOfFile[i];
                        File output = AudioConverter.getAbsoluteForOutputExtensionAndFolder(input, outputfolder, ".mp3");
                        System.out.println("processing: " + output.getAbsolutePath());
                        AudioConverter.mp3(input, output, listener);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
        })

                .start();
    }

    public static void batchConvertToWAV(File[] inputfiles, final File outputfolder,
                                         final EncoderProgressListener listener) {
        new Thread(new Runnable() {
            public void run() {
                File[] arrayOfFile;
                // int j = (arrayOfFile = AudioConverter.this).length;
                int j = (arrayOfFile = inputfiles).length;
                for (int i = 0; i < j; i++) {
                    try {
                        File input = arrayOfFile[i];
                        File output = AudioConverter.getAbsoluteForOutputExtensionAndFolder(input, outputfolder, ".wav");
                        System.out.println("processing: " + output.getAbsolutePath());
                        AudioConverter.wav(input, output, listener);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
        })

                .start();
    }

    public static void convertToMP3(File inputfile, final File outputfile, final EncoderProgressListener listener) {
        new Thread(new Runnable() {
            public void run() {
                // AudioConverter.mp3(AudioConverter.this, outputfile, listener);
                try {
                    AudioConverter.mp3(inputfile, outputfile, listener);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void convertToWAV(File inputfile, final File outputfile, final EncoderProgressListener listener) {
        new Thread(new Runnable() {
            public void run() {
                // AudioConverter.wav(AudioConverter.this, outputfile, listener);
                try {
                    AudioConverter.wav(inputfile, outputfile, listener);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void mp3(File inputfile, File outputfile, EncoderProgressListener listener) throws MalformedURLException {
        ArrayList<MultimediaObject> multis = new ArrayList<>();
        EncodingAttributes ea = new EncodingAttributes();

        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("libmp3lame");
        audio.setBitRate(mp3bitrate);
        audio.setChannels(channels);
        audio.setSamplingRate(samplerate);
        ea.setOutputFormat("mp3");
        ea.setAudioAttributes(audio);
        Encoder encoder = new Encoder();

        multis.add(new MultimediaObject(inputfile.toURI().toURL()));

        try {
            if (listener != null) {
                encoder.encode(multis, outputfile, ea, listener);
            } else {
                encoder.encode(multis, outputfile, ea);
            }
        } catch (IllegalArgumentException | EncoderException e) {
            JOptionPane.showMessageDialog(null,
                    "Input file formatting/encoding is incompatible\n" + inputfile.getName(), "Input File incompatible",
                    0);
            listener.progress(1001);
            e.printStackTrace();
        }
    }

    private static void wav(File inputfile, File outputfile, EncoderProgressListener listener) throws MalformedURLException {
        ArrayList<MultimediaObject> multis = new ArrayList<>();
        EncodingAttributes ea = new EncodingAttributes();

        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("pcm_s16le");
        ea.setOutputFormat("wav");
        ea.setAudioAttributes(audio);
        Encoder encoder = new Encoder();

        multis.add(new MultimediaObject(inputfile.toURI().toURL()));

        try {
            if (listener != null) {
                encoder.encode(multis, outputfile, ea, listener);
            } else {
                encoder.encode(multis, outputfile, ea);
            }
        } catch (IllegalArgumentException | EncoderException e) {
            JOptionPane.showMessageDialog(null,
                    "Input file formatting/encoding is incompatible\n" + inputfile.getName(), "Input File incompatible",
                    0);
            listener.progress(1001);
            e.printStackTrace();
        }
    }

    private static File getAbsoluteForOutputExtensionAndFolder(File inputfile, File outputfolder, String dotext) {
        String filename = inputfile.getName();
        int period = filename.lastIndexOf('.');
        if (period > 0) {
            filename = filename.substring(0, period) + dotext;
        }
        return new File(outputfolder + File.separator + filename);
    }
}
