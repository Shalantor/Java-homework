/*Class that implements the Seam Carving algorithm*/

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.Scanner;

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
        URL inputLink = null;           //case we get a link
        File inputFile = null;          //case we get a path to a file
        String path = null;
        Scanner input = new Scanner(System.in);

        /*TODO:It is stated in the excercise what we have ask user
        for a valid input if he doesn't enter one when starting the programm*/

        /*check if user has given command line arguments*/

        if(args.length == 0){
            System.out.println("Please enter a valid path to an existing image");
            System.out.println("or enter a valid url");
            path = input.nextLine();
        }
        else{
            path = args[0];
        }

        /*First try to open image from a link*/
        try{
            inputLink = new URL(path);
            try{
                test = new SeamCarver(inputLink);
            }
            catch(IOException e){
                System.out.println("Couldn't open picture from given link");
            }
         }
        catch(MalformedURLException e){
            System.out.println("Url is malformed");
        }

         /*Then try to open an existing image*/

        inputFile = new File(path);
        try{
            test = new SeamCarver(inputFile);
        }
        catch(IOException e){
            System.out.println("Couldn't open given file");
        }



    }
}
