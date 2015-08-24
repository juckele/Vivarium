package com.johnuckele.vivarium.scripts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import org.json.JSONException;

import com.johnuckele.vivarium.scripts.json.JSONConverter;
import com.johnuckele.vivarium.serialization.MapSerializer;

public class ScriptIO
{
    public static void saveSerializer(MapSerializer serializer, String fileName, Format f)
    {
        if (f == Format.JSON)
        {
            saveObjectWithJSON(serializer, fileName);
        }
        else
        {
            throw new Error("Loading format " + f + " is not supported");
        }
    }

    public static void saveStringToFile(String dataString, String fileName)
    {
        try
        {
            File file = new File(fileName);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] jsonByteData = dataString.getBytes();
            fos.write(jsonByteData);
            fos.flush();
            fos.close();
        }
        catch (IOException e)
        {
            System.out.print("Unable to write the file " + fileName + "\n");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static String loadFileToString(String fileName)
    {
        try
        {
            File file = new File(fileName);
            Scanner scanner = new Scanner(file);
            String dataString = scanner.useDelimiter("\\Z").next();
            scanner.close();
            return dataString;
        }
        catch (FileNotFoundException e)
        {
            System.out.print("Unable to read the file " + fileName + "\n");
            e.printStackTrace();
            System.exit(1);
            return null; // Unreachable, but Java doesn't know this.
        }
    }

    private static void saveObjectWithJSON(MapSerializer serializer, String fileName)
    {
        try
        {
            String jsonString = JSONConverter.serializerToJSONString(serializer);
            saveStringToFile(jsonString, fileName);
        }
        catch (JSONException e)
        {
            System.out.print("Unable to write the create JSON\n");
            e.printStackTrace();
            System.exit(2);
        }
    }

    public static Object loadObject(String fileName, Format f)
    {
        if (f == Format.JSON)
        {
            return loadObjectWithJSON(fileName);
        }
        else
        {
            throw new Error("Loading format " + f + " is not supported");
        }
    }

    private static Object loadObjectWithJSON(String fileName)
    {
        String jsonString = loadFileToString(fileName);
        return JSONConverter.jsonStringtoSerializer(jsonString);
    }
}