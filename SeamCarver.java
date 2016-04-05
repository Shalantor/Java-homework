/*Class that implements the Seam Carving algorithm*/

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class SeamCarver{

    /*the image to be processed*/
    private BufferedImage inputImage;
    private BufferedImage resizedImage;
    private Graphics2D graphics;

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

    /*TODO:change to private after testing*/
    /*Method to scale image before applying Seam carve algorithm*/
    public void scale(int width,int height){
        resizedImage = new BufferedImage(width, height, BufferedImage.TRANSLUCENT);
        graphics = resizedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(inputImage,0,0,width,height,null);
        graphics.dispose();
        System.out.println("New Height:" + resizedImage.getHeight() + " New Width:" + resizedImage.getWidth());
    }






    /*Main method*/
    public static void main(String[] args){

        SeamCarver seam = null;
        URL inputLink = null;           //case we get a link
        File inputFile = null;          //case we get a path to a file
        String path = null;
        Scanner input = new Scanner(System.in);
        int newWidth = 0;
        int newHeight = 0;
        String destinationFile;

        /*TODO:It is stated in the excercise what we have ask user
        for a valid input if he doesn't enter one when starting the programm*/

        /*check if user has given command line arguments*/

        if(args.length == 0){
            System.out.println("Please enter a valid path to an existing image");
            System.out.print("or enter a valid URL:");
            path = input.nextLine();
        }
        else{
            path = args[0];
        }

        /*First try to open image from a link*/
        try{
            inputLink = new URL(path);
            try{
                seam = new SeamCarver(inputLink);
            }
            catch(IOException e){
                System.out.println("Couldn't open picture from given link");
                System.exit(0);
            }
         }
        catch(MalformedURLException e){         //if url is malformed, try to interpret
            inputFile = new File(path);         //input as a path to a local file
            try{
                seam = new SeamCarver(inputFile);
            }
            catch(IOException ex){
                System.out.println("Couldn't open given file or URL is malformed");
                System.exit(0);
            }
        }

        /*TODO:optimize WORKAROUND of nextInt() not consuming \n character*/
        /*Ask for desired dimensions of image to create*/

        try{
            System.out.print("Please enter desired width:");
            newWidth = input.nextInt();
            input.nextLine();                   //consume \n character
        }
        catch(InputMismatchException e){
            System.out.println("This is not a valid integer");
            System.exit(0);
        }

        try{
            System.out.print("Please enter desired height:");
            newHeight = input.nextInt();
            input.nextLine();                   //consume \n character
        }
        catch(InputMismatchException e){
            System.out.println("This is not a valid integer");
            System.exit(0);
        }

        seam.scale(newWidth,newHeight);

        /*ask for name of file to store the results of calculation*/
        System.out.print("Please enter the name of destination file (must be a .png file):");
        destinationFile = input.nextLine();

    }
}
