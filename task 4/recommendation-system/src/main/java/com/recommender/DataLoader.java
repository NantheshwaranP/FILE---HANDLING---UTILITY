package com.recommender;

import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * Loads user-item rating data into a Mahout DataModel.
 * Supports both file path and classpath resource loading.
 */
public class DataLoader {

    // Maps item IDs to human-readable product names
    public static final Map<Long, String> PRODUCT_NAMES = Map.of(
        101L, "Laptop",
        102L, "Phone",
        103L, "Headphones",
        104L, "Tablet",
        105L, "Smartwatch",
        106L, "Camera",
        107L, "Speaker",
        108L, "Monitor",
        109L, "Keyboard",
        110L, "Mouse"
    );

    /**
     * Loads the DataModel from the ratings.csv bundled in resources.
     * Mahout's FileDataModel requires a real File, so we copy the resource to a temp file.
     */
    public static DataModel loadFromResource(String resourcePath) throws IOException {
        InputStream is = DataLoader.class.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }

        // Copy to a temp file (FileDataModel needs a real File, not a stream)
        Path tempFile = Files.createTempFile("ratings", ".csv");
        Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        tempFile.toFile().deleteOnExit();

        return new FileDataModel(tempFile.toFile());
    }

    /**
     * Loads the DataModel from an explicit file path.
     */
    public static DataModel loadFromFile(String filePath) throws IOException {
        return new FileDataModel(new File(filePath));
    }

    public static String getProductName(long itemId) {
        return PRODUCT_NAMES.getOrDefault(itemId, "Unknown Product #" + itemId);
    }
}
