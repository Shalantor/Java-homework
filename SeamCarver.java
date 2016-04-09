/*Class that implements the Seam Carving algorithm*/

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class SeamCarver{

    /*the image to be processed*/
    private BufferedImage inputImage;
    private Graphics2D graphics;
    private static double [][] energyTable;
    private int[] seam; //seam with min sum which includes numbers of chosen columns

    /*primary Constructor, which will also be used be the other 2
    It also throws an IOException because it maybe was called from one
    of the other 2 contructors*/

    public SeamCarver(BufferedImage image) throws IOException{

        inputImage = image;

        /*TODO:Remove line below this one after testing that it works*/
        System.out.println("Height:" + inputImage.getHeight() + " Width:" + inputImage.getWidth());

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
    /*NOTE: correct syntex is Color(column,row)*/
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
            adjustedRow = inputImage.getHeight() - 1;
        }

        if(adjustedCol < 0){
            adjustedCol = inputImage.getWidth() - 1;
        }

        /*Colors of surrounding pixels*/
        Color left = new Color(inputImage.getRGB( adjustedCol , row ));
        Color right = new Color(inputImage.getRGB( (col+1) % inputImage.getWidth() , row ));
        Color top = new Color(inputImage.getRGB( col , adjustedRow));
        Color bottom = new Color(inputImage.getRGB( col , (row+1) % inputImage.getHeight() ));

        /*Calculating energy*/
        energyX = Math.pow( left.getRed() - right.getRed() , 2 ) + Math.pow( left.getGreen() - right.getGreen() , 2 )
                    + Math.pow( left.getBlue() - right.getBlue() , 2 );

        energyY = Math.pow( top.getRed() - bottom.getRed() , 2 ) + Math.pow( top.getGreen() - bottom.getGreen() , 2 )
                    + Math.pow( top.getBlue() - bottom.getBlue() , 2 );

        energy = energyY + energyX ;

        return energy;

    }


    /*TODO:change to private after testing*/
    /*Method to scale image before applying Seam carve algorithm
    width, height = dimensions of image after scaling*/
    public void scale(int width,int height){


        /*Setting up what we need for scaling*/
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TRANSLUCENT);
        graphics = resizedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(inputImage,0,0,width,height,null);       //0,0--->x,y coordinate,null -->ImageObserver
        graphics.dispose();

        /*NOTE:Next line is just for testing if resize worked, remove after testing*/
        System.out.println("New Height:" + resizedImage.getHeight() + " New Width:" + resizedImage.getWidth());
        inputImage = resizedImage;
    }


    /*Method to remove vertical seam from picture*/
    public void removeVerticalSeam(int[] seam){

        //create the new image
        BufferedImage newImage = new BufferedImage(inputImage.getWidth() - 1,
                                inputImage.getHeight() , inputImage.getType());

        WritableRaster newImageRaster = newImage.getRaster() ;

        //get Data of original image
        Raster data = inputImage.getData();

        //garbage array TODO:check use of garbage array
        float[] garbArray = new float[inputImage.getWidth() * 3];

        //copy one row at a time
        for(int row = 0; row < seam.length; row ++){
            int column = seam[row];

            if(column > 0){                             //remove pixels from the left side of seam
                newImageRaster.setPixels(0,row,column,1,
                                        data.getPixels(0, row, column, 1, garbArray));
            }

            if(seam[row] < inputImage.getWidth() - 1){  //remove pixels from the right side of seam
                int widthAfter = newImage.getWidth() - column;
                newImageRaster.setPixels(column, row, widthAfter, 1,
                                        data.getPixels(column + 1, row, widthAfter, 1, garbArray));
            }

        }

        inputImage = newImage;

        System.out.println("ImageWIdth after crop: " + inputImage.getWidth());
    }


    /*Method for applying Seam Carve algorithm onto picture
    width, height = dimensions of the image we want to get after applying the seamCarve algorithm*/
    public void seamCarve(int width,int height){

        /*First get ratio of width/height, because we have to choose in which dimension
        to scale*/
        float ratio = (float)inputImage.getWidth() / inputImage.getHeight() ;

        /*Then calculate the respective width and height we would get for each choice*/
        int scaledWidth = Math.round(height * ratio) ;
        int scaledHeight = Math.round(width / ratio) ;

        System.out.println("ScaledHeight is " + scaledHeight + " and scaledWidth is " + scaledWidth);

        /*First check if scaling to one Dimension doesnt make the other one
        smaller than the result of seamcarve algorithm dimensions,
        then if that doesn't happen with any dimension choose the one that will
        need less iterations to complete, by choosing the smallest difference*/
        /*TODO: check if Math.ceil() has better results because for example Math.round(3.1) gives 3*/
        if(scaledWidth - width < 0){
            scaledWidth = width;
        }
        else if(scaledHeight - height < 0){
            scaledHeight = height;
        }
        else if(scaledWidth - width <= scaledHeight - height){
            scaledHeight = height;
        }
        else{
            scaledWidth = width;
        }

        /*Scale image*/

        this.scale(scaledWidth,scaledHeight);





    }

    //method creates or updates energyTable by using energy()
    public void createEnergyTable(){

        int width, height;

        width = inputImage.getWidth(); //updated value of newWidth
        height = inputImage.getHeight(); //updated value of newHeight

        energyTable = new double[height][width];

        /*Iterating over pixels of image*/
        for (int i=0; i < height; i++){
            for (int j=0; j < width; j++){
                energyTable[i][j] = energy(i,j);
            }
        }

        /*NOTE:print is only for testing, remove when finished*/
        System.out.println("ENERGYMAP");
        for (int i=0; i<inputImage.getHeight(); i++){
            System.out.println(Arrays.toString(energyTable[i]));
        }
    }


    //method finds vertical seam. Returns seamTable which includes column numbers of image.
    public int[] findVerticalSeam(){


        int height = inputImage.getHeight();
        int width = inputImage.getWidth();
        double bottom,bottomLeft,bottomRight;                                   //energies of pixels below the one we are checking
        int[] favoredSeam = null;
        double favoredSeamEnergy = 0;
        int[] checkSeam ;                                                       //current Seam that is checked
        double checkSeamEnergy;                                                 //current sum of Seam that is checked
        int column;
        double minEnergy;

        /*Find the seam with the lowest energy*/

        for( int i = 0 ; i < width ; i++ ){                                     //iterating over COLUMNS
            column = i;
            checkSeam = new int[height];
            checkSeam[0] = column ;                                              //add column to seam
            checkSeamEnergy = energy (0,column);                                //add energy of that column

            for ( int j = 0; j < height - 1 ; j++ ){                                //iterating over ROWS

                /*Getting energies of pixels*/
                bottom = energy( j+1 , column);
                bottomRight = energy( j+1 , (column + 1) % width );
                bottomLeft = energy( j+1 , ( column -1 + width ) % width) ;

                minEnergy = Math.min( Math.min( bottom , bottomLeft ) , bottomRight);

                if( minEnergy == bottomRight ){                                 //if minEnergy == bottom, column stays the same
                    column = (column + 1) % width ;
                }
                else if (minEnergy == bottomLeft ){
                    column = (column - 1 + width) % width ;
                }

                checkSeam[j+1]= column;                                          //update seam
                checkSeamEnergy += minEnergy ;                                  //update energy of seam

            }

            if(favoredSeamEnergy == 0 || favoredSeamEnergy > checkSeamEnergy ){ //is energy lower?
                favoredSeam = checkSeam;
                favoredSeamEnergy = checkSeamEnergy;
            }

        }

        System.out.println("LOWEST SEAM");
        System.out.println(favoredSeamEnergy);



        return(favoredSeam);
    }

    //method finds vertical seam. Returns seamTable which includes column numbers of image.
    public int[] findHorizontalSeam(){


        int height = inputImage.getHeight();
        int width = inputImage.getWidth();
        double bottom,bottomLeft,bottomRight;                                   //energies of pixels below the one we are checking
        int[] favoredSeam = null;
        double favoredSeamEnergy = 0;
        int[] checkSeam ;                                                       //current Seam that is checked
        double checkSeamEnergy;                                                 //current sum of Seam that is checked
        int row;
        double minEnergy;

        /*Find the seam with the lowest energy*/

        for( int i = 0 ; i < height ; i++ ){                                     //iterating over ROWS
            row = i;
            checkSeam = new int[width];
            checkSeam[0] = row ;                                              //add column to seam
            checkSeamEnergy = energy (row,0);                                //add energy of that column

            for ( int j = 0; j < width - 1 ; j++ ){                                //iterating over COLUMNS

                /*Getting energies of pixels*/
                bottom = energy( row , j+1 );
                bottomRight = energy( (row + 1) % height , j+1 );
                bottomLeft = energy( ( row -1 + height ) % height , j+1) ;

                minEnergy = Math.min( Math.min( bottom , bottomLeft ) , bottomRight);

                if( minEnergy == bottomRight ){                                 //if minEnergy == bottom, column stays the same
                    row = (row + 1) % height ;
                }
                else if (minEnergy == bottomLeft ){
                    row = (row - 1 + height) % height ;
                }

                checkSeam[j+1]= row;                                          //update seam
                checkSeamEnergy += minEnergy ;                                  //update energy of seam

            }

            if(favoredSeamEnergy == 0 || favoredSeamEnergy > checkSeamEnergy ){ //is energy lower?
                favoredSeam = checkSeam;
                favoredSeamEnergy = checkSeamEnergy;
            }

        }

        System.out.println("LOWEST SEAM");
        System.out.println(favoredSeamEnergy);



        return(favoredSeam);
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
        int[] test ;

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

        /*If file with the same name as detinationFile exists print an error message
        end exit the programm*/
        System.out.print("Please enter the name of destination file (this has no effect):");
        destinationPath = input.nextLine();
        destinationFile = new File(destinationPath);

        if(destinationFile.isFile()){
            System.out.println("File already exists");
            System.exit(0);
        }

        /*NOTE: Just for testing purposes, remove after successfull testing*/
        seam.seamCarve(newWidth,newHeight);

        seam.createEnergyTable();
        /*NOTE:print is only for testing, remove when finished*/
        //System.out.println("length of energyTable:" + energyTable.length);
        //System.out.println("And length of each of its elements: " + energyTable[0].length);

        test = seam.findVerticalSeam();
        seam.removeVerticalSeam(test);

        //PRINTS olny for testing
        System.out.println("VERTICALSEAM");
        System.out.println(Arrays.toString(test));
        System.out.println("LENGTH OF VERTICAL SEAM");
        System.out.println(test.length);

    }
}
