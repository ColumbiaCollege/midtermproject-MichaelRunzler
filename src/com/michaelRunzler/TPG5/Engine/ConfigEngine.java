package com.michaelRunzler.TPG5.Engine;

import core.CoreUtil.ARKJsonParser.ARKJsonParser;
import core.system.ARKAppCompat;

import java.io.File;

public class ConfigEngine
{
    public static final String FILE_NAME = "3BPConfig";

    private ARKJsonParser parser;
    private File root;

    public ConfigEngine(){
        this(ARKAppCompat.getOSSpecificAppPersistRoot());
    }

    public ConfigEngine(File rootDir)
    {
        this.root = new File(rootDir, FILE_NAME + ARKAppCompat.CONFIG_FILE_EXTENSION);
    }
}
