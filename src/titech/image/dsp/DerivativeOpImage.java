package titech.image.dsp;

import java.awt.*;
import java.awt.image.*;
import javax.media.jai.*;
import java.util.Map;

/**
  * OpImage implementation for "Derivative" operator.
  *
  *
  * @author David Gavilan
  */
public class DerivativeOpImage extends PointOpImage {

    /** Differenciate respect X, Y, both, or second derivatives.
     *  Only X and Y implemented right now.
     */
    public static final int X=1, Y=2, XX=3, YY=4, XY=5;

  protected int type;
 
  /**
    * Constructs an OpImage representing a derivative.
    * @param type Direction and such.
    */
  public DerivativeOpImage(RenderedImage source, ImageLayout layout,
         Map config,int type) {
     super(source,layout,config,true);
     
     this.type = type;
  }

  /**
    * Performs a derivative operation on a specified rectangle. Doing so, we will
    * be applying a derivative to each tile of the image.
    * Be careful then on tile margins.
    * @param sources an array of source Rasters, guaranteed to provide all
    *                necessary source data for computing the output. In this
    *                case, just one Raster.
    * @param dest a WritableRaster containing the area to be computed
    * @param destRect the rectangle within dest to be processed
    */
  public void computeRect(Raster sources[], WritableRaster dest,
          Rectangle destRect) {
  
     RasterFormatTag[] formatTags = getFormatTags();
     
     // destination accessor
     RasterAccessor dst = new RasterAccessor(dest, destRect, 
        formatTags[1], getColorModel());

     // the resulting image will be float (more accuracy before
     // the quantization process)
     if (dst.getDataType() != DataBuffer.TYPE_FLOAT) {
        throw new IllegalArgumentException("Supports float data only.");
     }
     
     // source accessor
     RasterAccessor src = new RasterAccessor(sources[0],
        mapDestRect(destRect, 0), formatTags[0],
	getSourceImage(0).getColorModel());

     if (src.getDataType() != DataBuffer.TYPE_FLOAT) {
        throw new IllegalArgumentException("Non-float source not supported yet.");
     }
     
     // get destination dimensions
     int dwidth = dst.getWidth();
     int dheight = dst.getHeight();
     int dnumBands = dst.getNumBands();        
     
     // get destination data array references and strides
     float dstDataArrays[][] = dst.getFloatDataArrays();
     int dstBandOffsets[] = dst.getBandOffsets();
     int dstPixelStride = dst.getPixelStride();
     int dstScanlineStride = dst.getScanlineStride();

     // get source data array references and strides     
     float srcDataArrays[][] = src.getFloatDataArrays();
     int srcBandOffsets[] = src.getBandOffsets();
     //int srcPixelStride = src.getPixelStride();
     int srcScanlineStride = src.getScanlineStride();

     for (int k=0;k<dnumBands;k++) {
       float dstData[] = dstDataArrays[k];
       float srcData[] = srcDataArrays[k];
       int srcScanlineOffset = srcBandOffsets[k];
       int dstScanlineOffset = dstBandOffsets[k];
                    
       int sslo = srcScanlineOffset;
       int sps = dstPixelStride;
       int ssls = srcScanlineStride;
        
       switch (type) {
       case X:
	   diffX(srcData, dstData, sslo, dstScanlineOffset,
		 sps, dstPixelStride, ssls, dstScanlineStride, dwidth, dheight );
	   break;
       case Y:	   
	   diffY(srcData, dstData, sslo, dstScanlineOffset,
		 sps, dstPixelStride, ssls, dstScanlineStride, dwidth, dheight );
	   break;
       }
     }
     
     
     if (dst.isDataCopy()) {
       dst.clampDataArrays();
       dst.copyDataToRaster();
     }
  }

  /** Applies the derivative horizontally.
    * @param srcData[] the source image data
    * @param dstData[] where to store the transformed image
    * @param srcScanlineOffset in which scan-line the source starts
    * @param dstScanlineOffset the scan-line offset from where to start to store
    * @param srcPixelStride the distance between consecutive pixels in the source
    * @param dstPixelStride the distance between consecutive pixels in the destination
    * @param srcScanlineStride the distance between two scanlines in the source
    * @param dstScanlineStride the distance between two scanlines in the destination   
    */
  private void diffX(float srcData[], float dstData[],
		     int srcScanlineOffset, int dstScanlineOffset,
		     int srcPixelStride, int dstPixelStride,
		     int srcScanlineStride, int dstScanlineStride,
		     int sizex, int sizey) {
    
      float ant, sig;   
      
      for (int j = 0; j<sizey; j++) {
	  int srcPixelOffset = srcScanlineOffset;
	  int dstPixelOffset = dstScanlineOffset;
	
	  ant = srcData[srcPixelOffset];
	  sig = srcData[srcPixelOffset+srcPixelStride];
	  dstData[dstPixelOffset]=(sig-ant)/2f;
	  srcPixelOffset += srcPixelStride;
	  dstPixelOffset += dstPixelStride;

	  for (int i = 1; i<sizex-1; i++) {

	      ant = srcData[srcPixelOffset-srcPixelStride];
	      sig = srcData[srcPixelOffset+srcPixelStride];           

	      dstData[dstPixelOffset] = (sig-ant)/2f;

	      srcPixelOffset += srcPixelStride;
	      dstPixelOffset += dstPixelStride;
	   
	  }
	  ant = srcData[srcPixelOffset-srcPixelStride];
	  sig = srcData[srcPixelOffset];
	  dstData[dstPixelOffset]=(sig-ant)/2f;
	  srcPixelOffset += srcPixelStride;
	  dstPixelOffset += dstPixelStride;

	  srcScanlineOffset += srcScanlineStride;
	  dstScanlineOffset += dstScanlineStride;	 
      }
  }

  /** Applies the derivative vertically.
    * @param srcData[] the source image data
    * @param dstData[] where to store the transformed image
    * @param srcScanlineOffset in which scan-line the source starts
    * @param dstScanlineOffset the scan-line offset from where to start to store
    * @param srcPixelStride the distance between consecutive pixels in the source
    * @param dstPixelStride the distance between consecutive pixels in the destination
    * @param srcScanlineStride the distance between two scanlines in the source
    * @param dstScanlineStride the distance between two scanlines in the destination   
    */
  private void diffY(float srcData[], float dstData[],
		     int srcScanlineOffset, int dstScanlineOffset,
		     int srcPixelStride, int dstPixelStride,
		     int srcScanlineStride, int dstScanlineStride,
		     int sizex, int sizey) {
    
      float ant, sig;   
      
      for (int i = 0; i<sizex; i++) {
	  int srcPixelOffset = srcScanlineOffset;
	  int dstPixelOffset = dstScanlineOffset;
	
	  ant = srcData[srcPixelOffset];
	  sig = srcData[srcPixelOffset+srcScanlineStride];
	  dstData[dstPixelOffset]=(sig-ant)/2f;
	  srcPixelOffset += srcScanlineStride;
	  dstPixelOffset += dstScanlineStride;

	  for (int j = 1; j<sizey-1; j++) {

	      ant = srcData[srcPixelOffset-srcScanlineStride];
	      sig = srcData[srcPixelOffset+srcScanlineStride];           

	      dstData[dstPixelOffset] = (sig-ant)/2f;

	      srcPixelOffset += srcScanlineStride;
	      dstPixelOffset += dstScanlineStride;
	   
	  }
	  ant = srcData[srcPixelOffset-srcScanlineStride];
	  sig = srcData[srcPixelOffset];
	  dstData[dstPixelOffset]=(sig-ant)/2f;
	  srcPixelOffset += srcScanlineStride;
	  dstPixelOffset += dstScanlineStride;

	  srcScanlineOffset += srcPixelStride;
	  dstScanlineOffset += dstPixelStride;	 
      }
  }
}
