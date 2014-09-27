package ocr;
import java.io.BufferedWriter;
import java.io.File;  
import java.io.FileNotFoundException;  
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import net.sourceforge.tess4j.*;
/**
 * Handle the Optical Character Recognition processing for FAA pdf files.
 * @author Kevin Dittmar
 */
public class OCR
{
    public static void main(String[] args) {
        String property = System.getProperty("sun.arch.data.model");
        System.setProperty("jna.library.path", 
            "32".equals(property) ? "lib/win32-x86" : "lib/win32-x86-64");
        for (String arg : args)
        {
            File imageFile = new File(arg);
            Tesseract instance = Tesseract.getInstance();  // JNA Interface Mapping
            //Tesseract1 instance = new Tesseract1(); // JNA Direct Mapping
            try {
                String result = instance.doOCR(imageFile);
                result = result.replaceAll("^(\\w)", "");
                formatOutput(result, arg);
                //System.out.println(result);
            } catch (TesseractException e) {
                System.err.println(e.getMessage());
            }
        }
    }
    public static void formatOutput(String tess_out, String file_name)
    {
        //Change the extension from .pdf to .xml.
        file_name = file_name.replaceAll(".pdf", ".xml");
        Scanner scanner = new Scanner(tess_out);
        String next_line;
        boolean header_finished = false;
        boolean instructions_finished = false;
        String xml_file = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<plate>\n<heading>\n";
        while (scanner.hasNextLine() && !header_finished)
        {
            next_line = scanner.nextLine();
            xml_file += next_line + "\n";
            //Header is done if we see DEPARTURE, DIAGRAM, or CITY, STATE
            if (next_line.contains("DEPARTURE") ||             
                next_line.contains("DIAGRAM") ||
                next_line.matches(".*[a-zA-Z]+, [a-zA-Z]+.*"))
            {
                header_finished = true;
            }
        }
        xml_file += "</heading>\n<instructions>\n";
        //We want to start off blank so we can save the last line for
        //special instructions.
        next_line = "";
        while (scanner.hasNextLine() && !instructions_finished)
        {
            xml_file += next_line + "\n";
            next_line = scanner.nextLine();
            if (next_line.contains("CAUTION") ||
                next_line.contains("SPECIAL"))
            {
                instructions_finished = true;
            }
        }
        xml_file += "</instructions>\n<special>\n";
        //This contains the special instructions piece found earlier.
        xml_file += next_line + "\n";
        while (scanner.hasNextLine())
        {
            xml_file += scanner.nextLine() + "\n";
        }
        xml_file += "</special>\n</plate>";
        xml_file = xml_file.replaceAll("\n *", "\n");
        xml_file = xml_file.replaceAll("\n+", "\n");
        //Write XML file
        try {
          File file = new File(file_name);
          BufferedWriter output = new BufferedWriter(new FileWriter(file));
          output.write(xml_file);
          output.close();
        } catch ( IOException e ) {
           e.printStackTrace();
        }

    }
}