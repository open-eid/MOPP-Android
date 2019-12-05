package ee.ria.DigiDoc.configuration.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import timber.log.Timber;

public class FileUtils {

    public static String readFileContent(String filePath) {
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            return readFileContent(fileInputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read content of cached file '" + filePath + "'", e);
        }
    }

    public static String readFileContent(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder sb = new StringBuilder();
            int i;
            while((i = reader.read()) != -1) {
                sb.append((char) i);
            }
            return sb.toString().trim();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read content of cached file", e);
        }
    }

    public static byte[] readFileContentBytes(String filePath) {
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            return readFileContentBytes(fileInputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read content of cached file '" + filePath + "'", e);
        }
    }

    public static byte[] readFileContentBytes(InputStream inputStream) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()){
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read content of cached file", e);
        }
    }

    public static void storeFile(String filePath, String content) {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file '" + filePath + "'!", e);
        }
    }

    public static void storeFile(String filePath, byte[] content) {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        try (FileOutputStream os = new FileOutputStream(file)) {
            os.write(content);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file '" + filePath + "'!", e);
        }
    }

    public static void createDirectoryIfNotExist(String directory) {
        File destinationDirectory = new File(directory);
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
        }
    }

    public static void writeToFile(BufferedReader reader, String destinationPath, String fileName) {
        try (FileOutputStream fileStream = new FileOutputStream(new File(destinationPath + File.separator + fileName));
             OutputStreamWriter writer = new OutputStreamWriter(fileStream)) {

            String fileLine;
            while ((fileLine = reader.readLine()) != null) {
                writer.write(fileLine + System.getProperty("line.separator"));
            }
        } catch (IOException e) {
            Timber.e(e, "Failed to open file: %s", fileName);
        }
    }
}
