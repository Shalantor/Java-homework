/*Class that implements the Seam Carving algorithm*/
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.*;
import java.io.*;
import java.lang.*;

public class SeamCarver{

    /*the image to be processed*/
    private BufferedImage inputImage;

    /*primary Constructor, which will also be used be the other 2
    It also throws an IOException because it maybe was called from one
    of the other 2 contructors*/

    public SeamCarver(BufferedImage image) throws IOException{

        inputImage = image;

        /*TODO:Remove line below this one after testing that it works*/
        System.out.println("Height:" + inputImage.getHeight() + "Width:" + inputImage.getWidth());

    }

    /*Constructor in case the user wants to open an existing file*/
    public SeamCarver(File file) throws IOException{

        this(ImageIO.read(file));
    }

    /*Constructor in case the user wants to open an image from an url*/
    public SeamCarver(URL link) throws IOException{

        this(ImageIO.read(link));
    }






    /*Main method*/
    public static void main(String[] args){

        SeamCarver test;
        URL inputLink = null;
        File inputFile = null;

        /*Frist try to open image from a link*/
        /*TODO:It is stated in the excercise what we have to keep asking user
        for a valid input until he does so, for testing purposes we just exit
        for now*/
        /*TODO:remove printStackTrace statements after testing*/

        try{
             inputLink = new URL(args[0]);
             try{
                 test = new SeamCarver(inputLink);
             }
             catch(IOException e){
                 e.printStackTrace();
             }
         }
         catch(MalformedURLException e){
             e.printStackTrace();
         }

         /*Then try to open an existing image*/

        inputFile = new File(args[0]);
        try{
            test = new SeamCarver(inputFile);
        }
        catch(IOException e){
            e.printStackTrace();
        }



    }
}
