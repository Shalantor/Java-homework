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
    private ArrayList< ArrayList<Double> > energyTable = new ArrayList< ArrayList<Double> >();
    private int[] seam;                         //seam with min sum which includes numbers of chosen columns
    private int width;                          //image width
    private int height;                         //image height
    private PrintWriter statsFile = null ;      //file to write the seams and image dimensions

    /*primary Constructor, which will also be used be the other 2
    It also throws an IOException because it maybe was called from one
    of the other 2 contructors*/

    public SeamCarver(BufferedImage image) throws IOException{

        inputImage = image;
        width = inputImage.getWidth();
        height = inputImage.getHeight();

        /*TODO:Remove line below this one after testing that it works*/
        System.out.println("Height:" + height + " Width:" + width);

    }

    /*Constructor in case the user wants to open an existing file*/
    //TODO:open PrintWriter with try statement
    public SeamCarver(File file) throws IOException{

        this(ImageIO.read(file));

        /*Get filename without extension*/
        String path = file.getName();

        int position = path.lastIndexOf(".");

        if(position > 0){
            path = path.substring(0,position);
        }

        statsFile = new PrintWriter( path + ".dbg" , "UTF-8" );

        statsFile.println("Starting Width: " + width + " Starting Height:" + height);

    }

    /*Constructor in case the user wants to open an image from an url*/
    public SeamCarver(URL link) throws IOException{

        this(ImageIO.read(link));

        /*Get filename from url without extension*/

        String path = link.getPath();
        int dotPosition = path.lastIndexOf(".");
        int slashPosition = path.lastIndexOf("/");

        path = path.substring( slashPosition + 1, dotPosition - 1 );
        statsFile = new PrintWriter( path + ".dbg" , "UTF-8" );

        statsFile.println("Starting Width: " + width + " Starting Height:" + height);



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

        /*Colors of surrounding pixels*/
        Color left = new Color(inputImage.getRGB( (col - 1 + width) % width, row ) );
        Color right = new Color(inputImage.getRGB( (col+1) % width , row ));
        Color top = new Color(inputImage.getRGB( col , (row -1 + height) % height ) );
        Color bottom = new Color(inputImage.getRGB( col , (row+1) % height ));

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
    private void scale(int width,int height){


        /*Setting up what we need for scaling*/
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TRANSLUCENT);
        graphics = resizedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(inputImage,0,0,width,height,null);       //0,0--->x,y coordinate,null -->ImageObserver
        graphics.dispose();

        /*storing resizedImage into inputImage*/
        inputImage = resizedImage;

        //Update image dimensions
        this.width = inputImage.getWidth();
        this.height = inputImage.getHeight();

        /*Writing to file*/
        statsFile.println("Width after Scale: " + width + " Height after scale: " + height);

        /*Just for testing remove after*/
        System.out.println("New Height: " + height + " New Width: " + width);

    }

    /*Method for removing horizontal seam from image*/
    public void removeHorizontalSeam(int[] seam){

        //create the new image
        BufferedImage newImage = new BufferedImage(width,
                                height -1 , inputImage.getType());

        WritableRaster newImageRaster = newImage.getRaster() ;

        //get Data of original image
        Raster data = inputImage.getData();

        //garbage array TODO:check use of garbage array
        float[] garbArray = new float[width * 10];

        //remove horizontal seam column by column
        for( int column = 0; column < seam.length; column ++){
            int row = seam[column];

            if(row > 0){                                //copy pixels above seam
                newImageRaster.setPixels(column,0,1,row,
                                        data.getPixels(column, 0 , 1 , row , garbArray));
            }


                if(seam[column] < height - 1){          //copy pixels under seam
                    int heightAfter = newImage.getHeight() - row ;
                    newImageRaster.setPixels(column , row ,  1 , heightAfter,
                                            data.getPixels(column , row + 1, 1 , heightAfter , garbArray));
                }


        }

        inputImage = newImage;

        //update image dimensions
        height = inputImage.getHeight();
        width = inputImage.getWidth();

    }

    /*Method for removing vertical seam from picture*/
    public void removeVerticalSeam(int[] seam){

        //create the new image
        BufferedImage newImage = new BufferedImage(width - 1,
                                height , inputImage.getType());

        WritableRaster newImageRaster = newImage.getRaster() ;

        //get Data of original image
        final Raster data = inputImage.getData();

        //garbage array TODO:check use of garbage array
        float[] garbArray = new float[height * 10];

        //copy one row at a time
        for(int row = 0; row < seam.length; row ++){

            int column = seam[row];

            if(column > 0){                             //copy pixels from the left side of seam
                newImageRaster.setPixels(0,row,column,1,
                                        data.getPixels(0, row, column, 1, garbArray));
            }

            if(seam[row] < width - 1){                  //copy pixels from the right side of seam
                int widthAfter = newImage.getWidth() - column;
                newImageRaster.setPixels(column , row, widthAfter, 1,
                                        data.getPixels(column + 1, row, widthAfter, 1, garbArray));
            }

        }

        inputImage = newImage;

        //update image dimensions
        width = inputImage.getWidth();
        height = inputImage.getHeight();

    }

    /*Method to update the energy table when a horizontal seam is removed
    pixels to update are the pixel below and above the seam , so 2 times the
    number of elements the seam has plus 2 for the pixel left of the first pixel
    of the seam and for the pixel right of the last pixel of the seam*/
    private void updateHorizontal(int[] seam){

        int[] abovePixels = new int[ seam.length];
        int[] belowPixels = new int[ seam.length];
        int i = 0;
        double newEnergy;
        int leftPixel = 0;
        int rightPixel = 0;

        //first left and right pixel
        if( seam[seam.length -1] != seam[0]){
            leftPixel = (seam[seam.length -1] +1 ) % height;
            if(leftPixel > seam[seam.length - 1 ]){
                leftPixel -= 1;
            }
            rightPixel = (seam[0] + 1) % height;
            if(rightPixel > seam[0]){
                rightPixel -= 1;
            }
        }

        //pixels above and below seam
        for(int row : seam){
            abovePixels[i] = (row - 1 + height) % height;       //top pixel
            belowPixels[i] = (row +1) % height;

            /*Adjust position*/
            if(abovePixels[i] > row){
                abovePixels[i] -= 1;
            }

            if(belowPixels[i] > row){
                belowPixels[i] -= 1;
            }

            //now remove pixelEnergy from table
            try{
                energyTable.get(row).remove(i);
            }
            catch(IndexOutOfBoundsException e){
                System.out.println("i: " + i + " row: " + row);
                System.out.println("W: " + width + " H: " + height);
                System.out.println(Arrays.toString(seam));
                System.exit(0);
            }
            i += 1;
        }

        //now update the energies of surrounding pixels of seam
        for(i = 0; i < width; i++){
            newEnergy = energy(abovePixels[i],i);
            energyTable.get(abovePixels[i]).set(i,newEnergy);
            newEnergy = energy(belowPixels[i],i);
            energyTable.get(belowPixels[i]).set(i,newEnergy);
        }

        if( seam[seam.length -1] != seam[0]){
            //far rightPixel
            newEnergy = energy(rightPixel,0);
            energyTable.get(rightPixel).set(0,newEnergy);
            //far left pixel
            newEnergy = energy(leftPixel,width-1);
            energyTable.get(leftPixel).set(width-1,newEnergy);
        }

    }

    /*Method to update energy table when a vertical seam is removed
    pixels to update are the pixels surrounding the seam , which are the pixels left and
    right from the seam and the one above the first pixel and also the one below the last pixelEnergy*/
    private void updateVertical(int[] seam){

        int[] leftPixels = new int[ seam.length ];
        int[] rightPixels = new int[ seam.length];
        double newEnergy;
        int i = 0;
        int topPixel = 0;
        int bottomPixel = 0;

        //first top and bottom pixel
        //width is correct dont change
        if( seam[seam.length -1 ] != seam[0]){
            topPixel = (seam[seam.length -1 ] + 1) % width;
            if(topPixel > seam[seam.length -1 ] ){
                topPixel -= 1;
            }
            bottomPixel = (seam[0] + 1) % width;
            if(bottomPixel > seam[0] ){
                bottomPixel -= 1;
            }
        }

        //pixels left and right from seam
        for(int column : seam){
            leftPixels[i] = (column - 1 + width) % width;       //left pixel
            rightPixels[i] = (column+1) % width ;               //right pixel

            /*Adjust position*/
            if(leftPixels[i] > column){
                leftPixels[i] -= 1;
            }

            if(rightPixels[i] > column){
                rightPixels[i] -= 1;
            }

            //now remove pixelEnergy from table
            energyTable.get(i).remove(column);
            i += 1;
        }

        //now update the energies of surrounding pixels of seam
        for(i = 0; i < height ; i++){
            newEnergy = energy(i,leftPixels[i]);
            energyTable.get(i).set(leftPixels[i],newEnergy);
            newEnergy = energy(i,rightPixels[i]);
            energyTable.get(i).set(rightPixels[i],newEnergy);
        }

        if( seam[seam.length -1 ] != seam[0]){
            //far bottom pixel
            newEnergy = energy(0,bottomPixel);
            energyTable.get(0).set(bottomPixel,newEnergy);
            //far top pixel
            newEnergy = energy(height-1,topPixel);
            energyTable.get(height-1).set(topPixel,newEnergy);
        }

    }

    /*Method for applying Seam Carve algorithm onto picture
    width, height = dimensions of the image we want to get after applying the seamCarve algorithm*/
    public void seamCarve(int width,int height){

        int[] foundSeam;

        /*First get ratio of width/height, because we have to choose in which dimension
        to scale*/
        float ratio = (float) this.width / this.height ;

        /*Then calculate the respective width and height we would get for each choice*/
        int scaledWidth = Math.round(height * ratio) ;
        int scaledHeight = Math.round(width / ratio) ;


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

        /*Create energytable*/
        this.createEnergyTable();

        /*apply seam carving algorithm*/
        if(height == scaledHeight){//remove vertical seams
            while(this.width > width ){
                foundSeam = findVerticalSeam();
                updateVertical(foundSeam);
                removeVerticalSeam(foundSeam);
            }
        }
        else{
            while(this.height > height){//remove horizontal seams
                foundSeam = findHorizontalSeam();
                updateHorizontal(foundSeam);
                removeHorizontalSeam(foundSeam);
            }
        }

        /*Close stats file*/
        statsFile.close();

    }

    //Method to store file
    //TODO:ask if changing constructors is allowed
    public void storeImage(File outputFile){

        try{
            ImageIO.write(inputImage,"png",outputFile);
        }
        catch(IOException e){
            System.out.println("Couldn't save file");
            System.exit(0);
        }

    }

    //method creates or updates energyTable by using energy()
    public void createEnergyTable(){

        ArrayList<Double> row ;
        energyTable = new ArrayList<ArrayList<Double>>();

        /*Iterating over pixels of image*/
        for (int i=0; i < height; i++){
            row = new ArrayList<Double> ();
            for (int j=0; j < width; j++){
                row.add( energy(i,j) );
            }
            energyTable.add(row);
        }

    }


    //method finds vertical seam. Returns seamTable which includes column numbers of image.
    public int[] findVerticalSeam(){

        //TODO:make bottompixel favored if all 3 have same energy


        double bottom,bottomLeft,bottomRight;                                   //energies of pixels below the one we are checking
        int[] favoredSeam = null;
        double favoredSeamEnergy = -1;
        int[] checkSeam ;                                                       //current Seam that is checked
        double checkSeamEnergy;                                                 //current sum of Seam that is checked
        int column;
        double minEnergy;

        /*Find the seam with the lowest energy*/

        for( int i = 0 ; i < width ; i++ ){              //iterating over COLUMNS
            column = i;
            checkSeam = new int[height];
            checkSeam[0] = column ;                      //add column to seam
            checkSeamEnergy = energyTable.get(0).get(column);         //add energy of that column

            for ( int j = 0; j < height - 1 ; j++ ){     //iterating over ROWS

                /*Getting energies of pixels*/
                bottom = energyTable.get( j+1 ).get( column );
                bottomRight = energyTable.get(j+1).get( (column + 1) % width );
                bottomLeft = energyTable.get( j+1 ).get( ( column -1 + width ) % width) ;

                /*Calculate min energy*/
                minEnergy = Math.min( Math.min( bottom , bottomLeft ) , bottomRight);

                //Always prefer bottom pixel if all have the same energy*/
                if(minEnergy == bottom){
                }
                else if( minEnergy == bottomRight ){
                    column = (column + 1) % width ;
                }
                else if (minEnergy == bottomLeft ){
                    column = (column - 1 + width) % width ;
                }

                checkSeam[j+1]= column;                                         //update seam
                checkSeamEnergy += minEnergy ;                                  //update energy of seam

            }

            if(favoredSeamEnergy < 0 || favoredSeamEnergy > checkSeamEnergy ){ //is energy lower?
                favoredSeam = checkSeam;
                favoredSeamEnergy = checkSeamEnergy;
            }

        }

        statsFile.println(Arrays.toString(favoredSeam));
        return(favoredSeam);
    }

    //method finds vertical seam. Returns seamTable which includes column numbers of image.
    public int[] findHorizontalSeam(){

        double bottom,bottomLeft,bottomRight;                                   //energies of pixels below the one we are checking
        int[] favoredSeam = null;
        double favoredSeamEnergy = -1;
        int[] checkSeam ;                                                       //current Seam that is checked
        double checkSeamEnergy;                                                 //current sum of Seam that is checked
        int row;
        double minEnergy;

        /*Find the seam with the lowest energy*/

        for( int i = 0 ; i < height ; i++ ){                        //iterating over ROWS
            row = i;
            checkSeam = new int[width];
            checkSeam[0] = row ;                                    //add column to seam
            checkSeamEnergy = energyTable.get(row).get(0);                    //add energy of that column

            for ( int j = 0; j < width - 1 ; j++ ){                 //iterating over COLUMNS

                /*Getting energies of pixels*/
                bottom = energyTable.get(row).get(j+1);
                bottomRight = energyTable.get( (row + 1)  % height ).get( j+1 );
                bottomLeft = energyTable.get( ( row -1 + height ) % height ).get( j+1) ;

                minEnergy = Math.min( Math.min( bottom , bottomLeft ) , bottomRight);


                /*Always prefer bottom pixel if all have the sam eenergy*/
                if(minEnergy == bottom){
                }
                else if( minEnergy == bottomRight ){
                    row = (row + 1) % height ;
                }
                else if (minEnergy == bottomLeft ){
                    row = (row - 1 + height) % height ;
                }

                checkSeam[j+1]= row;                                          //update seam
                checkSeamEnergy += minEnergy ;                                //update energy of seam

            }

            if(favoredSeamEnergy < 0 || favoredSeamEnergy > checkSeamEnergy ){ //is energy lower?( en < 0 is for start)
                favoredSeam = checkSeam;
                favoredSeamEnergy = checkSeamEnergy;
            }

        }

        statsFile.println(Arrays.toString(favoredSeam));
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
        and exit the programm*/
        System.out.print("Please enter the name of destination file :");
        destinationPath = input.nextLine();
        destinationFile = new File(destinationPath + ".png");

        if(destinationFile.isFile()){
            System.out.println("File already exists");
            System.exit(0);
        }

        seam.seamCarve(newWidth,newHeight);

        seam.storeImage(destinationFile);



    }
}
