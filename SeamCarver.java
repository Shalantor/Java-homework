/*George Karaolanis 1841
  Pantelis Dimitroulis 1770
  University of Thessaly
  summer semester 2016
  Object oriented programming assignment*/

/*Class that implements the Seam Carving algorithm.
NOTE:instead of calculating the energy for each pixel
on each step, only the affected pixel's energy is
calculated again. To achieve this several lists are created
each representing either the energy for each pixel in a
row or in a column, depending on the type of seams we removed.
These lists are stored in a bigger list, in a way that they can
be accessed easily by using the get(index) method these Arraylists
provide*/

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
    private ArrayList< ArrayList<Double> > energyTable = new ArrayList< ArrayList<Double> >();
    private int width;                          /*image width*/
    private int height;                         /*image height*/
    private PrintWriter statsFile = null ;      /*file to write the seams and image dimensions*/
    private String fileName = null;             /*name of destinationFile for our image*/

    /*primary Constructor, which will also be used be the other 2
    It also throws an IOException because it maybe was called from one
    of the other 2 contructors*/

    public SeamCarver(BufferedImage image) throws IOException{

        inputImage = image;
        width = inputImage.getWidth();
        height = inputImage.getHeight();

        System.out.println("The image's dimensions are:" + width + "x" + height);

    }


    /*Constructor in case the user wants
    to open an existing image from a directory*/
    public SeamCarver(File file) throws IOException{

        this(ImageIO.read(file));

        /*Get filename without extension*/
        String path = file.getName();

        int position = path.lastIndexOf(".");

        if(position > 0){
            path = path.substring(0,position);
        }

        fileName = path;

        /*create file for statistics*/
        statsFile = new PrintWriter( path + ".dbg" , "UTF-8" );

        statsFile.println("INIT DIMENSIONS:" + width + "x" + height);

    }


    /*Constructor in case the user wants to open an image from an url*/
    public SeamCarver(URL link) throws IOException{

        this(ImageIO.read(link));

        /*Get filename from url without extension*/

        String path = link.getPath();
        int dotPosition = path.lastIndexOf(".");
        int slashPosition = path.lastIndexOf("/");

        path = path.substring( slashPosition + 1, dotPosition - 1 );

        fileName = path;

        /*Create file for statistics*/
        statsFile = new PrintWriter( path + ".dbg" , "UTF-8" );

        statsFile.println("INIT DIMENSIONS:" + width + "x" + height);

    }


    /*Method for calculating the energy of a pixel*/
    public double energy(int row,int col){

        /*Energy on X and Y axis respectfully*/
        double energyX;
        double energyY;

        /*overall energy of a pixel*/
        double energy;

        /*RGB Color values of surrounding pixels*/
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


    /*Method to scale image before applying Seam carve algorithm
    width, height = dimensions of image after scaling*/
    private void scale(int width,int height){

        /*Creating new BufferedImage*/
        Image resizedImage = inputImage.getScaledInstance(width, height, Image.SCALE_DEFAULT);

        /*convert image to bufferedImage*/
        inputImage = new BufferedImage(resizedImage.getWidth(null), resizedImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        /* Draw the image onto the buffered image*/
        Graphics2D bGr = inputImage.createGraphics();
        bGr.drawImage(resizedImage, 0, 0, null);
        bGr.dispose();

        /*Update image dimensions*/
        this.width = inputImage.getWidth();
        this.height = inputImage.getHeight();

        /*Writing to statistics file*/
        statsFile.println("SCALE DIMENSIONS:" + width + "x" + height);

    }


    /*Method for removing horizontal seam from image*/
    public void removeHorizontalSeam(int[] seam){

        /*create the new image*/
        BufferedImage newImage = new BufferedImage(width,
                                height -1 , inputImage.getType());

        /*Iterate over each pixel and if a pixel isn't part of
        the seam to remove, draw it onto the new image*/
        for (int column = 0; column < width; column++){
            int copyPosition = 0;
            for(int row = 0; row < height; row++){
                if(row != seam[column]){
                    newImage.setRGB(column,copyPosition, inputImage.getRGB(column,row));
                    copyPosition++;
                }
            }
        }

        /*Store new Image into our inputImage*/
        inputImage = newImage;

        /*update image dimensions*/
        height = inputImage.getHeight();
        width = inputImage.getWidth();

    }


    /*Method for removing vertical seam from picture*/
    public void removeVerticalSeam(int[] seam){

        /*create the new image*/
        BufferedImage newImage = new BufferedImage(width - 1,
                                height , inputImage.getType());

        /*Iterave over each pixel and if it isn'a a part of the
        seam to remove, draw it onto the new image*/
        for( int row =0; row < height; row++){
            int copyPos = 0;
            for(int column =0; column < width; column++){
                if(column != seam[row]){
                    newImage.setRGB(copyPos, row, inputImage.getRGB(column,row));
                    copyPos++;
                }
            }
        }

        inputImage = newImage;

        /*update image dimensions*/
        width = inputImage.getWidth();
        height = inputImage.getHeight();

    }


    /*Method to update the energy list when a horizontal seam is removed.
    Pixels to update are the pixels below and above the seam , so 2 times the
    number of elements the seam has plus 2 for the pixel left of the first pixel
    of the seam and for the pixel right of the last pixel of the seam, assuming
    that the numeration of the seam goes from left to right on the image.
    Positions are adjusted sometimes, because when we remove the seam, the corresponding
    energies also get removed, so we have to adjust the positions of the pixels that
    have their energy changed*/
    private void updateHorizontal(int[] seam){

        /*arrays to store the pixels which energy must be updated*/
        int[] abovePixels = new int[ seam.length];
        int[] belowPixels = new int[ seam.length];
        int i = 0;
        double newEnergy;
        int leftPixel = 0;
        int rightPixel = 0;

        /*first update the energy of the two extra pixels*/
        if( seam[seam.length -1] != seam[0]){                           /*Check if those pixels are part of the seam*/

            leftPixel = seam[seam.length - 1];
            if(leftPixel > seam[0]){                                    /*adjust position*/
                leftPixel -= 1;
            }

            rightPixel = seam[0];
            if(rightPixel > seam[seam.length -1]){                      /*Adjust position*/
                rightPixel -= 1;
            }
        }

        /*pixels above and below seam*/
        for(int row : seam){
            abovePixels[i] = (row - 1 + height) % height;
            belowPixels[i] = (row +1) % height;

            /*Adjust position*/
            if(abovePixels[i] > row){
                abovePixels[i] -= 1;
            }

            if(belowPixels[i] > row){
                belowPixels[i] -= 1;
            }

            /*now remove pixelEnergy from table*/
            energyTable.get(i).remove(row);
            i += 1;
        }

        /*now update the energies of surrounding pixels of seam*/
        for(i = 0; i < width; i++){
            newEnergy = energy(abovePixels[i],i);
            energyTable.get(i).set(abovePixels[i],newEnergy);
            newEnergy = energy(belowPixels[i],i);
            energyTable.get(i).set(belowPixels[i],newEnergy);
        }

        if( seam[seam.length -1] != seam[0]){
            /*far rightPixel*/
            newEnergy = energy(rightPixel,0);
            energyTable.get(0).set(rightPixel,newEnergy);
            /*far left pixel*/
            newEnergy = energy(leftPixel,width-1);
            energyTable.get(width-1).set(leftPixel,newEnergy);
        }

    }


    /*Method to update energy list when a vertical seam is removed.
    Pixels to update are the pixels surrounding the seam , which are
    the pixels left and right from the seam and the one above the first
    pixel and also the one below the last pixel, assuming that the seam
    from top to bottom of the image.Positions are adjusted sometimes,
    because when we remove the seam, the corresponding energies also get
     removed, so we have to adjust the positions of the pixels that
    have their energy changed*/
    private void updateVertical(int[] seam){

        /*Arrays to store the position of pixels whose energy must be updated*/
        int[] leftPixels = new int[ seam.length ];
        int[] rightPixels = new int[ seam.length];
        double newEnergy;
        int i = 0;
        int topPixel = 0;
        int bottomPixel = 0;

        /*first top and bottom pixel*/
        if( seam[seam.length -1 ] != seam[0]){

            topPixel = seam[seam.length - 1];
            if(topPixel > seam[0]){                         /*Adjust position*/
                topPixel -= 1;
            }

            bottomPixel = seam[0];
            if(bottomPixel > seam[seam.length - 1]){        /*Adjust position*/
                bottomPixel -= 1;
            }
        }

        /*pixels left and right from seam*/
        for(int column : seam){
            leftPixels[i] = (column - 1 + width) % width;
            rightPixels[i] = (column+1) % width ;

            /*Adjust position*/
            if(leftPixels[i] > column){
                leftPixels[i] -= 1;
            }

            if(rightPixels[i] > column){
                rightPixels[i] -= 1;
            }

            /*now remove pixelEnergy from table*/
            energyTable.get(i).remove(column);
            i += 1;
        }

        /*now update the energies of surrounding pixels of seam*/
        for(i = 0; i < height ; i++){
            newEnergy = energy(i,leftPixels[i]);
            energyTable.get(i).set(leftPixels[i],newEnergy);
            newEnergy = energy(i,rightPixels[i]);
            energyTable.get(i).set(rightPixels[i],newEnergy);
        }

        if( seam[seam.length -1 ] != seam[0]){
            /*far bottom pixel*/
            newEnergy = energy(0,bottomPixel);
            energyTable.get(0).set(bottomPixel,newEnergy);
            /*far top pixel*/
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
        scale(scaledWidth,scaledHeight);

        /*Write the final dimensions into the statistics file*/
        statsFile.println("SEAMCARVE DIMENSIONS:" + width + "x" + height);

        /*apply seam carving algorithm*/
        if(height == scaledHeight){                     /*remove vertical seams*/
            createEnergyTable("vertical");
            while(this.width > width ){
                foundSeam = findVerticalSeam();
                updateVertical(foundSeam);
                removeVerticalSeam(foundSeam);
            }
        }
        else{
            createEnergyTable("horizontal");
            while(this.height > height){                /*remove horizontal seams*/
                foundSeam = findHorizontalSeam();
                updateHorizontal(foundSeam);
                removeHorizontalSeam(foundSeam);
            }
        }

        /*Close stats file*/
        statsFile.close();

        /*Rename file*/
        File oldName = new File(fileName + ".dbg");
        File newName = new File(fileName + "_" + width + "x" + height + ".dbg");
        oldName.renameTo(newName);


    }


    /*Method to store the image into a file*/
    public void storeImage(File outputFile){

        try{
            ImageIO.write(inputImage,"png",outputFile);
        }
        catch(IOException e){
            System.out.println("Couldn't save file, an IOException occured!");
            System.exit(0);
        }

    }


    /*This method creates or updates energy list by using energy() method.
    If horizontal seams are to be removed, each list represents one column
    and these list are all stored in one large list. On the other side if
    vertical seams are to be removed, then each list represents a row and
    again we store these lists in one large list*/
    private void createEnergyTable(String type ){

        ArrayList<Double> row ;
        ArrayList<Double> column;
        energyTable = new ArrayList<ArrayList<Double>>();

        if(type == "vertical"){
            /*Iterating over pixels of image*/
            for (int i=0; i < height; i++){
                row = new ArrayList<Double> ();             /*Creating small list, which is one row*/
                for (int j=0; j < width; j++){
                    row.add( energy(i,j) );
                }
                energyTable.add(row);
            }
        }
        else{
            for (int i=0; i < width; i++){
                column = new ArrayList<Double> ();          /*Creating small list, which is one column*/
                for (int j=0; j < height; j++){
                    column.add( energy(j,i) );
                }
                energyTable.add(column);
            }
        }

    }


    /*This method finds the vertical seam with
    the lowest energy from top to bottom*/
    public int[] findVerticalSeam(){

        double bottom,bottomLeft,bottomRight;       /*energies of pixels below the one we are checking*/
        int[] favoredSeam = null;
        double favoredSeamEnergy = -1;              /*Instantiate -1 for first iteration over candidate seams*/
        int[] checkSeam ;                           /*current Seam that is checked*/
        double checkSeamEnergy;                     /*current sum of Seam that is checked*/
        int column;
        double minEnergy;

        /*Find the seam with the lowest energy*/

        for( int i = 0 ; i < width ; i++ ){                           /*iterating over COLUMNS*/
            column = i;
            checkSeam = new int[height];
            checkSeam[0] = column ;                                   /*add number of column to seam*/
            checkSeamEnergy = energyTable.get(0).get(column);         /*for adding energy of pixel*/

            for ( int j = 0; j < height - 1 ; j++ ){                  /*iterating over ROWS*/

                /*Getting energies of pixels*/
                bottom = energyTable.get( j+1 ).get( column );
                bottomRight = energyTable.get(j+1).get( (column + 1) % width );
                bottomLeft = energyTable.get( j+1 ).get( ( column -1 + width ) % width) ;

                /*Calculate min energy*/
                minEnergy = Math.min( Math.min( bottom , bottomLeft ) , bottomRight);

                /*Prefer bottom pixel if all have the same energy*/
                if(minEnergy == bottom){
                }
                else if( minEnergy == bottomRight ){
                    column = (column + 1) % width ;
                }
                else if (minEnergy == bottomLeft ){
                    column = (column - 1 + width) % width ;
                }

                checkSeam[j+1]= column;                                         /*update seam*/
                checkSeamEnergy += minEnergy ;                                  /*update energy of seam*/

            }

            if(favoredSeamEnergy < 0 || favoredSeamEnergy > checkSeamEnergy ){ /*is energy lower?*/
                favoredSeam = checkSeam;
                favoredSeamEnergy = checkSeamEnergy;
            }

        }

        /*Write seam into file*/
        for(int position : favoredSeam){
            statsFile.format("%d ", position);
        }

        /*New line character*/
        statsFile.println("");


        return(favoredSeam);
    }


    /*Method that finds the horizontal seam to remove */
    public int[] findHorizontalSeam(){

        double right,topRight,bottomRight;           /*energies of pixels next to the one we are examining*/
        int[] favoredSeam = null;
        double favoredSeamEnergy = -1;
        int[] checkSeam ;                               /*current Seam that is examined*/
        double checkSeamEnergy;                         /*current sum of Seam that is examined*/
        int row;
        double minEnergy;

        /*Find the seam with the lowest energy*/

        for( int i = 0 ; i < height ; i++ ){                        /*iterating over ROWS*/
            row = i;
            checkSeam = new int[width];
            checkSeam[0] = row ;                                    /*add row number to seam*/
            checkSeamEnergy = energyTable.get(0).get(row);          /*add energy of pixel in that row*/

            for ( int j = 0; j < width - 1 ; j++ ){                 /*iterating over COLUMNS*/

                /*Getting energies of pixels*/
                right = energyTable.get(j+1).get(row);
                bottomRight = energyTable.get( j+1 ).get( (row + 1)  % height  );
                topRight = energyTable.get( ( j+1 )).get( ( row -1 + height ) % height ) ;

                minEnergy = Math.min( Math.min( right, topRight ) , bottomRight);


                /*Always prefer right pixel if all have the sam eenergy*/
                if(minEnergy == right){
                }
                else if( minEnergy == bottomRight ){
                    row = (row + 1) % height ;
                }
                else if (minEnergy == topRight ){
                    row = (row - 1 + height) % height ;
                }

                checkSeam[j+1]= row;                                          /*update seam*/
                checkSeamEnergy += minEnergy ;                                /*update energy of seam*/

            }

            if(favoredSeamEnergy < 0 || favoredSeamEnergy > checkSeamEnergy ){ /*is energy lower?*/
                favoredSeam = checkSeam;
                favoredSeamEnergy = checkSeamEnergy;
            }

        }

        /*Write to statistics file*/
        for(int position: favoredSeam){
            statsFile.format("%d ", position);
        }

        /*Newline character*/
        statsFile.println("");

        return(favoredSeam);
    }

    /*return height of image*/
    public int getHeight(){
        return height;
    }

    /*return width of image*/
    public int getWidth(){
        return width;
    }


    /*Main method*/
    public static void main(String[] args){

        SeamCarver seam = null;
        URL inputLink = null;                       /*case we get a link*/
        File inputFile = null;                      /*case we get a path to a file*/
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
        catch(MalformedURLException e){         /*if url is malformed, try to interpret*/
            inputFile = new File(path);         /*input as a path to a local file*/
            try{
                seam = new SeamCarver(inputFile);
            }
            catch(IOException ex){
                System.out.println("Couldn't open given file or URL is malformed");
                System.exit(0);
            }
        }

        /*Ask user for desired width*/
        while(true){

            try{
                System.out.print("Please enter desired width:");
                newWidth = input.nextInt();
                input.nextLine();                   /*consume \n character*/
            }
            catch(InputMismatchException e){
                System.out.println("This is not a valid integer");
                input.nextLine();                   /*clear buffer*/
                continue;
            }

            /*Check if user has given a greater width value than that of the image or negative*/
            if(newWidth > seam.getWidth() || newWidth < 0){
                System.out.println("Accepted values are 1 - " + seam.getWidth());
                continue;
            }

            break;

        }

        /*Ask user for desired height*/
        while(true){
            try{
                System.out.print("Please enter desired height:");
                newHeight = input.nextInt();
                input.nextLine();                   /*consume \n character*/
            }
            catch(InputMismatchException e){
                System.out.println("This is not a valid integer");
                input.nextLine();                   /*clear buffer*/
                continue;
            }

            /*Check if user has given a greater height value than that of the image or negative*/
            if(newHeight > seam.getHeight() || newHeight < 0){
                System.out.println("Accepted values are 1 - " + seam.getHeight());
                continue;
            }

            break;

        }

        /*If file with the same name as detinationFile exists print an error message*/

        while(true){
            System.out.print("Please enter the name of destination file :");
            destinationPath = input.nextLine();
            destinationFile = new File(destinationPath + ".png");

            if(destinationFile.isFile()){
                System.out.println("A file with this name already exists");
                continue;
            }

            break;
        }

        /*Apply seam carcing algorithm*/
        seam.seamCarve(newWidth,newHeight);

        /*Store image*/
        seam.storeImage(destinationFile);

    }
}
