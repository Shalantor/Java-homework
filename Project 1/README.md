# Java-homework1
ce325 First homework assignment
object-oriented programming using Java
University of Thessaly
Summer semester 2016

When changing dimensions of an image it is sometimes desirable that the changes will only be done in one dimension. For example, someone may want to to get a narrower image from an already existing one, without changing its height. Respectively someone may want to change the height of an image without also changing its width.

The easiest way to do the above, would be to either crop the image in one dimension or to scale it. Cropping the image has the disadvantage of also removing some important content from it and scaling in only one dimension may squeeze its content , deforming the actual result image.

In contradiction to the above two methods, the Seam Carving algorithm, invented by Shai Avidan and Ariel Shamir in 2007, has the advantage that it can change the size of an image but also be aware of its content when doing so. The Seam Carving algorithm generally creates better results when editing pictures where the important content is primarily in a small region inside the image. For example that's the case with images depicting landscapes with a group of people or a building.


Explanation of algorithm:

Suppose that the image is a two dimensional array of H rows and W columns (WxH), where the letters H and W represent the height and the width of the image respectively. Having that in mind , an exact copy of that array is created storing the "energy" of the image. The images energy is calculated for each pixel individually, using the expression below:

                                Ε(i,j) = Eχ(i,j) + Εy(i,j),

Ex(i,j) = [R(i,j+1) - R(i,j-1)]^2 + [G(i,j+1) - G(i,j-1)]^2 + [B(i,j+1) - B(i,j-1)]^2

Ey(i,j) = [R(i+1,j) - R(i-1,j)]^2 + [G(i+1,j) - G(i-1,j)]^2 + [B(i+1,j) - B(i-1,j)]^2

Where i stands for row, j for column , Ex for the energy on the X-axis , Ey for the energy on the Y-axis and E is the sum of those two. R,G and B represent the RGB values of a pixel for the colors RED , GREEN and BLUE.

In this assignment , when a pixel is on the far right,left,top or bottom, it is assumed that the pixels are arranged in a circular way. So when a pixel is on the far right of the image(let's call it X) with a column index of W - 1 , the pixel right from pixel X, will be the the one in the same row with a column index of 0. The same applies to the other dimensions.

Having calculated the energies of all pixels, we have to find the seam which pixels have the lowest energy. A seam is a path of pixels from one side of the image to the other. When reducing the width of an image we look for a vertical seam and when reducing the height we look for a horizontal seam.
When having found the seam which has the lowest sum of its pixels energy , it is removed from the image and the same process starts all over again until the desired image dimensions are reached. Note that when removing a seam, the energy of some pixels change.
