package titech.image.dsp;

import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import javax.media.jai.*;
import javax.media.jai.registry.RenderedRegistryMode;
import javax.media.jai.registry.RenderableRegistryMode;
import java.util.Vector;
import javax.media.jai.util.Range;

/**
  * An <code>OperationDescriptor</code> describing the "Derivative" operation.
  *
  * <p><table border=1>
  * <caption>Resource List</caption>
  * <tr><th>Name</th><th>Value</th></tr>
  * <tr><th>GlobalName</th><td>Derivative</td></tr>
  * <tr><th>LocalName</th><td>Derivative</td></tr>
  * <tr><th>Description</th><td>Derivative Operator</td></tr>
  * <tr><th>DocURL</th><td>DerivativeDescriptor.html</td></tr>
  * <tr><th>Version</th><td>0.1</td></tr>
  * <tr><th>arg0Desc</th><td>Type.</td></tr>
  * </table></p>
  * <p><table border=1>
  * <caption>Parameter List</caption>
  * <tr><th>Name</th><th>Class Type</th><th>Default Value</th></tr>
  * <tr><td>type</td><td>java.lang.Integer</td><td>1</td></tr>
  * </table></p>  
  * @author David Gavilan
  */
public class DerivativeDescriptor extends OperationDescriptorImpl {

  private static final String[][] resources = {
     {"GlobalName", "Derivative"},
     {"LocalName",  "Derivative"},
     {"Description", "Differenciates an image."},
     {"DocURL",      "DerivativeDescriptor.html"},
     {"Version",     "0.1"},
     {"arg0Desc",    "Type."}
  };
  
  private static final Class[] paramClasses = {
      java.lang.Integer.class };
  private static final String[] paramNames = {
      "type" };
  private static final Object[] paramDefaults = {
      new Integer(1) };
  private static final Object[] validParamValues  = {      
      new Range(Integer.class, new Integer(1), new Integer(5)) 
      };
  
  public DerivativeDescriptor() {
    super(resources, new String[] {RenderedRegistryMode.MODE_NAME,
          RenderableRegistryMode.MODE_NAME}, 1,
	  paramNames, paramClasses, paramDefaults, validParamValues);
  }
  

    
}
