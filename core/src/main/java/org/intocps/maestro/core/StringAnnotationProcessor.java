package org.intocps.maestro.core;


import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class StringAnnotationProcessor {
    final static Logger logger = LoggerFactory.getLogger(StringAnnotationProcessor.class);

    public static String processStringAnnotations(File specificationFolder, String data) throws IOException {
        final String FILE_TAG = "@file:";
        if (data != null && data.trim().startsWith(FILE_TAG)) {
            String fileName = data.substring(data.indexOf(FILE_TAG) + FILE_TAG.length()).trim();
            File file = new File(fileName);

            if (!file.isAbsolute()) {
                //make relative from specification folder
                file = new File(specificationFolder, fileName);
            }

            if (file.exists() && file.isFile()) {

                data = StringEscapeUtils.escapeJava(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
            } else {
                logger.warn("Tag '" + FILE_TAG + "' not replaced in '" + data + "' file does not exist: '" + file.getAbsolutePath() + "'");
            }
        }
        return data;
    }
}
