package ocr;
import java.io.BufferedWriter;
import java.io.File;  
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
        String xml_file = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<plate>\n<raw>\n";
        String xml_attributes = "";
        while (scanner.hasNextLine())
        {
            next_line = scanner.nextLine();
            xml_file += next_line + "\n";
            if (next_line.contains("NOTE:"))
            {
                xml_attributes += "<note>\n" + next_line + "\n";
                next_line = scanner.nextLine();
                xml_file += next_line + "\n";
                while (!next_line.matches("^[A-Z :,.]*$"))
                {
                    xml_attributes += next_line + "\n";
                    next_line = scanner.nextLine();
                    xml_file += next_line + "\n";
                }
                xml_attributes += "</note>\n";
            }
            //Get diagram type
            if (next_line.contains("DIAGRAM"))
            {
                xml_attributes += "<type>" + next_line + "</type>\n";
            }
            //Get location.
            if (next_line.matches(".*[A-Z]+, [A-Z]+.*"))
            {
                xml_attributes += "<location>" + next_line + "</location>\n";
            }
            //Get departure route descriptions.
            if (next_line.contains("DEPARTURE ROUTE"))
            {
                xml_attributes += "<route_description>\n" + next_line + "\n";
                while (scanner.hasNextLine() && !next_line.matches(".*[.!?]$"))
                {
                    next_line = scanner.nextLine();
                    xml_attributes += next_line + "\n";
                }
                xml_attributes += "</route_description>\n";
            }
            //Get special instructions.
            if (next_line.contains("CAUTION") || next_line.contains("SPECIAL"))
            {
                xml_attributes += "<special>\n" + next_line + "\n";
                while (scanner.hasNextLine() && !next_line.matches(".*[.!?]$"))
                {
                    next_line = scanner.nextLine();
                    xml_attributes += next_line + "\n";
                }
                xml_attributes += "</special>\n";
            }
            if (next_line.matches(".*TAKE[OQ]FF.*"))
            {
                String tag = next_line.toLowerCase().replaceAll("[.:]", "");
                xml_attributes += "<" + tag + ">\n";
                //Get the first header line.
                next_line = scanner.nextLine();
                xml_file += next_line + "\n";
                while (scanner.hasNextLine() && 
                       (next_line.contains("NOTE:") || 
                       !next_line.matches("[A-Z :,.]*")))
                {
                    //We have a header line right now.
                    //Get all of the headers
                    String[] headers;
                    if (next_line.contains(","))
                    {
                        headers = next_line.split(":")[0].split(",");
                    }
                    else
                    {
                        headers = new String[1];
                        headers[0] = next_line.split(":")[0];
                    }
                    //Get some of the info
                    String info = next_line.split(":")[1];
                    next_line = scanner.nextLine();
                    xml_file += next_line + "\n";
                    //We might not have all of our info for these headers.
                    while (!next_line.contains(":") &&
                           !next_line.matches("^[A-Z :,.]*$"))
                    {
                        info += next_line + "\n";
                        next_line = scanner.nextLine();
                        xml_file += next_line + "\n";
                    }
                    for (String header : headers)
                    {
                        xml_attributes += "<" + header + ">" +
                                          info +
                                          "</" + header + ">\n";
                    }
                }
                xml_attributes += "</" + tag + ">\n";
            }
            //Get diagram name
            if (next_line.contains("DEPARTURE") && !next_line.contains("ROUTE"))
            {
                xml_attributes += "<diagram_name>" + next_line + "</diagram_name>\n";
            }
        }
        //End raw processing tag and append filtered attributes.
        xml_file += "</raw>\n" + xml_attributes;
        //End plate.
        xml_file += "</plate>\n";
        //Remove extra newlines and spaces.
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