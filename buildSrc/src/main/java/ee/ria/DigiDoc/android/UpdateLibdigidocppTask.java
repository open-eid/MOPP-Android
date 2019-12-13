package ee.ria.DigiDoc.android;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class UpdateLibdigidocppTask extends DefaultTask {

    private static final String PREFIX = "libdigidocpp.";
    private static final String TEST_TSL = "-testtsl";
    private static final String SUFFIX = ".zip";
    private static final String JAR = PREFIX + "jar";
    private static final String SCHEMA = "schema.zip";

    private static final List<String> ABIS = new ArrayList<>();
    private static final Map<String, String> ABI_FILES = new HashMap<>();
    private static final Map<String, String> ABI_DIRS = new HashMap<>();
    static {
        ABIS.add("arm64-v8a");
        ABIS.add("armeabi-v7a");
        ABIS.add("x86");

        ABI_FILES.put("arm64-v8a", "androidarm64");
        ABI_FILES.put("armeabi-v7a", "androidarm");
        ABI_FILES.put("x86", "androidx86");

        ABI_DIRS.put("arm64-v8a", "aarch64-linux-android");
//        ABI_DIRS.put("armeabi-v7a", "arm-linux-androideabi");
        ABI_DIRS.put("x86", "i686-linux-android");
    }

    private String dir = ".";

    @Option(option = "dir", description = "Directory where the libdigidocpp ZIP files are")
    public void setDir(String dir) {
        this.dir = dir;
    }

    @Input
    public String getDir() {
        return dir;
    }

    @TaskAction
    public void run() throws IOException {
        File inputDir = new File(getProject().getRootDir(), getDir());
        File outputDir = getTemporaryDir();
        delete(outputDir);

        AtomicBoolean generateJar = new AtomicBoolean(true);
        AtomicBoolean generateSchema = new AtomicBoolean(true);
        AtomicBoolean generateTestSchema = new AtomicBoolean(true);

        for (String abi : ABIS) {
            update(
                    new File(inputDir, PREFIX + ABI_FILES.get(abi) + SUFFIX),
                    new File(outputDir, "unzipped"),
                    abi,
                    false,
                    generateJar,
                    generateSchema
            );
            update(
                    new File(inputDir, PREFIX + ABI_FILES.get(abi) + TEST_TSL + SUFFIX),
                    new File(outputDir, "unzipped" + TEST_TSL),
                    abi,
                    true,
                    generateJar,
                    generateTestSchema
            );
        }
    }

    private void update(File zipFile, File outputDir, String abi, boolean isTestTsl, AtomicBoolean generateJar, AtomicBoolean generateSchema) throws IOException {
        if (!zipFile.exists()) {
            log("Could not find file %s", zipFile);
            return;
        }
        log("Updating from %s", zipFile);
        unzip(zipFile, outputDir);
        File cacheDir = new File(outputDir, PREFIX + ABI_FILES.get(abi));

        if (generateJar.getAndSet(false)) {
            log("Generating %s from %s", JAR, zipFile);
            File sourceDir = new File(cacheDir, "include");
            File jarFile = new File(cacheDir, JAR);
            compile(sourceDir);
            jar(sourceDir, jarFile);
            Files.copy(
                    jarFile.toPath(),
                    new File(getProject().getProjectDir(), "libs/" + JAR).toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
        if (generateSchema.getAndSet(false)) {
            log("Generating %s from %s", SCHEMA, zipFile);
            File schemaCacheDir = new File(cacheDir, "etc");
            if (isTestTsl) {
                unzip(getClass().getClassLoader().getResourceAsStream("testtsl.zip"), schemaCacheDir);
            }
            File schemaZipFile = new File(cacheDir, SCHEMA);
            ZipOutputStream schemaOutputStream = new ZipOutputStream(new FileOutputStream(schemaZipFile));
            for (File schemaFile : files(schemaCacheDir)) {
                ZipEntry entry = new ZipEntry(schemaFile.getName());
                entry.setTime(schemaFile.lastModified());
                schemaOutputStream.putNextEntry(entry);
                Files.copy(schemaFile.toPath(), schemaOutputStream);
                schemaOutputStream.closeEntry();
            }
            schemaOutputStream.close();
            File schemaDir = isTestTsl
                    ? new File(getProject().getProjectDir(), "src/envtest/res/raw")
                    : new File(getProject().getProjectDir(), "src/main/res/raw");
            Files.copy(
                    schemaZipFile.toPath(),
                    new File(schemaDir, SCHEMA).toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        if (isTestTsl) {
            Files.copy(
                    new File(cacheDir, "lib/libdigidoc_java.so").toPath(),
                    new File(getProject().getProjectDir(), "src/envtest/jniLibs/" + abi + "/libdigidoc_java.so").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } else {
            if (!abi.equals("armeabi-v7a")) {
                Files.copy(
                        new File(cacheDir, ABI_DIRS.get(abi) + "/lib/libc++_shared.so").toPath(),
                        new File(getProject().getProjectDir(), "src/main/jniLibs/" + abi + "/libc++_shared.so").toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
            Files.copy(
                    new File(cacheDir, "lib/libdigidoc_java.so").toPath(),
                    new File(getProject().getProjectDir(), "src/main/jniLibs/" + abi + "/libdigidoc_java.so").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
            Files.copy(
                    new File(cacheDir, "lib/libdigidoc_java.so").toPath(),
                    new File(getProject().getProjectDir(), "src/debug/jniLibs/" + abi + "/libdigidoc_java.so").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private void log(String message, Object... parameters) {
        getLogger().lifecycle(String.format(message, parameters));
    }

    private static void delete(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                delete(f);
            }
            file.delete();
        } else {
            file.delete();
        }
    }

    private static void unzip(File zip, File destination) throws IOException {
        unzip(new FileInputStream(zip), destination);
    }

    private static void unzip(InputStream stream, File destination) throws IOException {
        try (ZipInputStream inputStream = new ZipInputStream(stream)) {
            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                File entryFile = new File(destination, entry.getName());
                if (entry.isDirectory()) {
                    continue;
                }
                Files.createDirectories(entryFile.getParentFile().toPath());
                Files.copy(inputStream, entryFile.toPath());
            }
        }
    }

    private static void compile(File path) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        compiler.getTask(null, fileManager, null, null, null, fileManager.getJavaFileObjectsFromFiles(files(path))).call();
        fileManager.close();
    }

    private static void jar(File path, File jar) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(jar), manifest);
        for (File file : files(path)) {
            if (!file.getName().endsWith(".class")) {
                continue;
            }
            JarEntry entry = new JarEntry(path.toPath().relativize(file.toPath()).toString());
            entry.setTime(file.lastModified());
            outputStream.putNextEntry(entry);
            Files.copy(file.toPath(), outputStream);
            outputStream.closeEntry();
        }
        outputStream.close();
    }

    private static List<File> files(File dir) {
        List<File> files = new ArrayList<>();
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                files.addAll(files(file));
            } else {
                files.add(file);
            }
        }
        return files;
    }
}
