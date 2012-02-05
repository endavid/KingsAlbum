package titech.image.dsp;

import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import javax.media.jai.*;

/**
  * Class implementing the RIF interface for the Wavelet operator.
  * An instance of this class should be registered with the OperationRegistry
  * with operation name "Wavelet" and product name "ccd-hyper".
  */
public class WaveletRIF implements RenderedImageFactory {
   public WaveletRIF() {}
   
   public RenderedImage create(ParameterBlock paramBlock,
                               RenderingHints renderHints) {
      RenderedImage source = paramBlock.getRenderedSource(0);
      
      ImageLayout layout = renderHints == null ? null : 
                  (ImageLayout)renderHints.get(JAI.KEY_IMAGE_LAYOUT);
      
      String algorism = (String)paramBlock.getObjectParameter(0);
      int level = paramBlock.getIntParameter(1);
		  
      return new WaveletOpImage(source, layout, renderHints,
             algorism, level);
   }
}
