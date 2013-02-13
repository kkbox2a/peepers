package com.foxdogstudios.peepers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import static com.googlecode.javacv.cpp.opencv_core.cvFlip;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.OpenCVFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class WebcamStreamer implements Runnable
{
    private static final String CAPTURE_FILE = "capture.jpg";

    IplImage image;
    CanvasFrame canvas = new CanvasFrame("Web Cam");
    final MJpegRtpStreamer mJpegRtpStreamer;

    public WebcamStreamer() throws IOException
    {
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        mJpegRtpStreamer = new MJpegRtpStreamer();
    } // constructor

    @Override
    public void run()
    {
        FrameGrabber grabber = new OpenCVFrameGrabber("");
        try
        {
            grabber.start();
        } // try
        catch (com.googlecode.javacv.FrameGrabber.Exception e)
        {
            System.err.println("Could not start the webcam grabber");
            return;
        } //catch

        IplImage img;
        while (true)
        {
            /* We grab a frame from the camera and save
             * it to a file in jpeg format. We then read this file to
             * a buffer which we send using the jpeg streamer.
             */
            try
            {
                img = grabber.grab();
            } // try
            catch (com.googlecode.javacv.FrameGrabber.Exception e)
            {
                System.err.println("Could not get a video frame from the webcam");
                return;
            } // catch
            if (img == null)
            {
                continue;
            } // if

            // 90 degrees rotation anti clockwise
            cvFlip(img, img, 1);
            cvSaveImage(CAPTURE_FILE, img);

            RandomAccessFile jpegFile = null;
            final int jpegLength;
            byte[] jpegBuffer = null;
            try
            {
                jpegFile = new RandomAccessFile(CAPTURE_FILE, "r");
                jpegLength = (int) jpegFile.length();
                jpegBuffer = new byte[jpegLength];
                jpegFile.read(jpegBuffer);
            } // try
            catch (FileNotFoundException e)
            {
                System.err.println("Could not find file '" + CAPTURE_FILE + "'");
                return;
            } // catch
            catch (IOException e)
            {
                System.err.println("Could not read file '" + CAPTURE_FILE + "'");
                return;
            } // catch
            finally
            {
                if (jpegFile != null)
                {
                    try
                    {
                        jpegFile.close();
                    } // try
                    catch (IOException e)
                    {
                        System.err.println("Could not close jpeg file");
                    } // catch
                } // if
            } // finally

            try
            {
                mJpegRtpStreamer.sendJpeg(jpegBuffer, jpegLength, img.width(), img.height(),
                        System.currentTimeMillis());
            } // try
            catch (IOException e)
            {
                System.err.println("Could not send jpeg through socket");
                return;
            } // catch

            canvas.showImage(img);
        } // while
    } // run
}

