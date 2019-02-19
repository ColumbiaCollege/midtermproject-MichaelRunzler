package com.michaelRunzler.TPG5.Engine;

import core.CoreUtil.ARKJsonParser.ARKJsonElement;
import core.CoreUtil.ARKJsonParser.ARKJsonObject;
import core.CoreUtil.ARKJsonParser.ARKJsonParser;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import core.system.ARKAppCompat;

import java.io.*;

/**
 * Stores configuration data in a JSON data structure.
 */
public class ConfigEngine
{
    public static final String FILE_NAME = "3BPConfig";

    private File root;
    private XLoggerInterpreter log;
    public ARKJsonObject index;

    /**
     * Default constructor.
     * Uses the default persistent storage path specified in {@link ARKAppCompat} as the root directory.
     */
    public ConfigEngine(){
        this(ARKAppCompat.getOSSpecificAppPersistRoot());
    }

    /**
     * Standard constructor.
     * @param rootDir The parent directory to save the file in. Specifying a file pointer instead of
     *                a directory pointer will cause an IllegalArgumentException to be thrown.
     */
    public ConfigEngine(File rootDir)
    {
        if(rootDir.isFile()) throw new IllegalArgumentException("Root directory cannot be a file!");
        this.root = new File(rootDir, FILE_NAME + ARKAppCompat.CONFIG_FILE_EXTENSION);
        index = new ARKJsonObject("{\n\n}");
        index.parse(); // Preloads the JSON data table and flags the construct as editable and ready for encoding
        log = new XLoggerInterpreter("Configuration System");
    }

    /**
     * Set the config in memory back to its initial default settings.
     * Does not affect the config on disk.
     */
    public void loadDefaults()
    {

    }

    /**
     * Save the current config in memory to disk, creating the destination file if required,
     * or overwriting an existing one.
     * @return {@code true} if the save was successful, {@code false} if not
     */
    public boolean save()
    {
        FileOutputStream fos;
        try {
            // Attempt to (in order) create the parent directory if it does not exist,
            // delete an existing file if there is one, and create a new file.
            // If any of the operations fail, return.
            if(!root.getParentFile().exists() && !root.getParentFile().mkdirs()) return false;
            if(root.exists() && !root.delete()) return false;
            if(!root.createNewFile()) return false;
            fos = new FileOutputStream(root);
        } catch (IOException e) {
            log.logEvent(e);
            return false;
        }

        // Write the contents of the index to disk if there are any.
        if(index.getElementMap().size() != 0 || index.getArrayMap().size() != 0)
        {
            try {
                char[] json = index.getJSONText().toCharArray();
                for (char c : json) fos.write(c);
                fos.close();
            }catch (IOException e){
                log.logEvent(e);
                return false;
            }
        }else{
            return root.delete();
        }

        return true;
    }

    /**
     * Load a config from disk, replacing the current config in memory.
     * @return {@code true} if the load was successful, {@code false} if not
     */
    public boolean load()
    {
        if(!root.exists()) return false;

        try {
            index = ARKJsonParser.loadFromFile(root);
            index.parse();
        } catch (IOException e) {
            log.logEvent(e);
            return false;
        }

        return true;
    }
}
