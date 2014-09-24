package ocr;
import java.io.File;
import net.sourceforge.tess4j.*;
/**
 *
 * @author Kevin Dittmar
 */
public class OCR
{
    public static void main(String[] args) {
        String property = System.getProperty("sun.arch.data.model");
        System.setProperty("jna.library.path", 
            "32".equals(property) ? "lib/win32-x86" : "lib/win32-x86-64");
        File imageFile = new File("resources/ACY/00669ATLANTICCITY.pdf");
        Tesseract instance = Tesseract.getInstance();  // JNA Interface Mapping
        //Tesseract1 instance = new Tesseract1(); // JNA Direct Mapping

        try {
            String result = instance.doOCR(imageFile);
            System.out.println(result);
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }
    }
    
}
