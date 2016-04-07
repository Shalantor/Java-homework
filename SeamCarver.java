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


    /*Method for calculating the energy of a pixel*/
    public double energy(int row,int col){

        /*TODO:check if energy variable is necessary or if just better to
        calculate on the go*/
        /*Energy on X and Y axis respectfully*/
        double energyX;
        double energyY;
        double energy;

        /*In case we get out of bounds because of a negative index*/
        /*TODO:Check for a better way of doing this*/
        int adjustedCol = col - 1;
        int adjustedRow = row - 1;

        if(adjustedRow < 0){
            adjustedRow = resizedImage.getHeight() - 1;
        }

        if(adjustedCol < 0){
            adjustedCol = resizedImage.getWidth() - 1;
        }

        /*Colors of surrounding pixels*/
        Color left = new Color(resizedImage.getRGB(row,adjustedCol));
        Color right = new Color(resizedImage.getRGB(row , (col+1) % resizedImage.getWidth() ));
        Color top = new Color(resizedImage.getRGB(adjustedRow,col));
        Color bottom = new Color(resizedImage.getRGB( (row+1) % resizedImage.getHeight() ,col));

        /*Calculating energy*/
        energyX = Math.pow( left.getRed() - right.getRed() , 2 ) + Math.pow( left.getGreen() - right.getGreen() , 2 )
                    + Math.pow( left.getBlue() - right.getBlue() , 2 );

        energyY = Math.pow( top.getRed() - bottom.getRed() , 2 ) + Math.pow( top.getGreen() - bottom.getGreen() , 2 )
                    + Math.pow( top.getBlue() - bottom.getBlue() , 2 );

        energy = energyY + energyX ;

        return energy;

    }


    /*TODO:change to private after testing*/
    /*Method to scale image before applying Seam carve algorithm*/
    public void scale(int width,int height){

        /*Setting up what we need for scaling*/
        resizedImage = new BufferedImage(width, height, BufferedImage.TRANSLUCENT);
        graphics = resizedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(inputImage,0,0,width,height,null);       //0,0--->x,y coordinate,null -->ImageObserver
        graphics.dispose();

        /*NOTE:Next line is just for testing if resize worked, remove after testing*/
        System.out.println("New Height:" + resizedImage.getHeight() + " New Width:" + resizedImage.getWidth());
    }


    /*Method for applying Seam Carve algorithm onto picture*/
    public void seamCarve(int width,int height){

        /*Variables used for scaling*/
        int scaleFactor = 1;
        int scaleWidth = inputImage.getWidth();
        int scaleHeight = inputImage.getHeight();
        int inputWidth = scaleWidth;
        int inputHeight = scaleHeight;

        /*Calculating optimal scale dimensions*/
        /*TODO:look if there is a better way to to this*/
        while( ( inputWidth/scaleFactor >= width ) && ( inputHeight/scaleFactor >= height ) ){
            scaleWidth = inputWidth / scaleFactor;
            scaleHeight = inputHeight / scaleFactor;
            scaleFactor += 1;
        }

        this.scale(scaleWidth,scaleHeight);




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
        String destinationPath;
        File destinationFile;

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

        /*NOTE: Just for testing purposes, remove after successfull testing*/
        seam.seamCarve(newWidth,newHeight);

        /*If file with the same name as detinationFile exists print an error message
        end exit the programm*/
        System.out.print("Please enter the name of destination file (must be a .png file):");
        destinationPath = input.nextLine();
        destinationFile = new File(destinationPath);

        if(destinationFile.isFile()){
            System.out.println("File already exists");
            System.exit(0);
        }

    }
}
