package titech.image.dsp;

import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import javax.media.jai.*;

/**
  * Class implementing the RIF interface for the Derivative operator.
  * An instance of this class should be registered with the OperationRegistry
  * with operation name "Derivative" and product name "titech".
  */
public class DerivativeRIF implements RenderedImageFactory {
   public DerivativeRIF() {}
   
   public RenderedImage create(ParameterBlock paramBlock,
                               RenderingHints renderHints) {
      RenderedImage source = paramBlock.getRenderedSource(0);
      
      ImageLayout layout = renderHints == null ? null : 
                  (ImageLayout)renderHints.get(JAI.KEY_IMAGE_LAYOUT);
           
      int type = paramBlock.getIntParameter(0);
		  
      return new DerivativeOpImage(source, layout, renderHints,
             type);
   }
}
