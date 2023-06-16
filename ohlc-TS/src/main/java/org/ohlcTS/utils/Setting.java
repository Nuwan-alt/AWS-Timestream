package org.ohlcTS.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

public class Setting {
public Map<String,String> getSettings() throws FileNotFoundException {

    Yaml yaml = new Yaml();
    InputStream inputStream = new FileInputStream(new File("config/application.yml"));
    Map<String,String> output = yaml.load(inputStream);

    return output;
    }
}
