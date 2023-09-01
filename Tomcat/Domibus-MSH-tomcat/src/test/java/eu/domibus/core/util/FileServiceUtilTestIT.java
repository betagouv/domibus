package eu.domibus.core.util;

import eu.domibus.AbstractIT;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileServiceUtilTestIT extends AbstractIT {

    final File parentDir = new File("target/test");

    @Test
    public void testCopyFile() throws IOException {
        final File file = copyContentToFile();
        FileUtils.delete(file);
    }

    private File copyContentToFile() throws IOException {
        String content = "hello";
        final File outputFile = new File(parentDir, "myfile" + System.nanoTime() + ".txt");

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        new FileServiceUtilImpl().copyToFile(inputStream, outputFile);
        return outputFile;
    }

    @Disabled//comment to test the performance
    @Test
    public void stressTestCopyFile() throws IOException, InterruptedException {
        Runnable copyToFileRunnable = () -> {
            try {
                copyContentToFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        List<Thread> threads = new ArrayList<>();
        final long start = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            final Thread thread = new Thread(copyToFileRunnable);
            threads.add(thread);
            thread.start();
        }
        for (int i = 0; i < 500; i++) {
            threads.get(i).join();
        }

        final long duration = System.currentTimeMillis() - start;
        System.out.println("-------------Duration in ms " + duration);

        FileUtils.deleteDirectory(parentDir);
    }
}
