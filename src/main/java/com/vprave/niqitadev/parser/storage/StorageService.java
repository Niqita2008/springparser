package com.vprave.niqitadev.parser.storage;

import com.vprave.niqitadev.parser.parse.IllegalDocumentException;
import com.vprave.niqitadev.parser.parse.Parser;
import com.vprave.niqitadev.parser.table.Result;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import static com.vprave.niqitadev.parser.parse.IllegalDocumentException.IllegalCause.INCORRECT_FORMAT;
import static com.vprave.niqitadev.parser.parse.IllegalDocumentException.IllegalCause.INVALID;

@Service
public class StorageService {

    private final Path rootLocation;
    private final Logger logger;
    private final PerformanceMonitor performanceMonitor;
    private final SimpleDateFormat dateFormat;
    private final Parser parser;

    public StorageService(StorageProperties properties, PerformanceMonitor performanceMonitor, Parser parser) {
        this.performanceMonitor = performanceMonitor;
        rootLocation = properties.getLocation();
        dateFormat = properties.getDateFormat();
        this.parser = parser;
        logger = LogManager.getLogger("main");
    }

    public Result handleFile(MultipartFile file) throws IllegalDocumentException {
        PDDocument doc = null;
        try {
            if (file.isEmpty()) throw new StorageException("Failed to store empty file.");
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) throw new StorageException("Failed to store file with wrong name.");
            Path path = rootLocation.resolve(dateFormat.format(new Date())).normalize().toAbsolutePath();
            Files.createDirectory(path);
            File copied = new File(path.toFile(), originalFilename);
            file.transferTo(copied);

            logger.info("Starting detect from " + copied.getAbsolutePath());
            double l = System.nanoTime();
            RandomAccessRead rar = new RandomAccessReadBufferedFile(copied);
            Result result = parser.get(doc = new PDFParser(rar).parse());
            rar.close();
            logger.info("Parsing took " + ((System.nanoTime() - l) / 1000000000.) + "s");
            performanceMonitor.print();
            return result;
        } catch (Exception e) {
            System.gc();
            logger.error("Something went wrong", e);
            try {
                Objects.requireNonNull(doc).close();
            } catch (Exception exception) {
                throw new IllegalDocumentException(INVALID, exception);
            }
            throw new IllegalDocumentException(INCORRECT_FORMAT, e);
        }
    }

    public void init() {
        try {
            Files.createDirectories(rootLocation);
            Date date = new Date(System.currentTimeMillis() - 2592000000L); // -30 days
            File[] files = rootLocation.toFile().listFiles(file -> {
                try {
                    return file.isDirectory() && dateFormat.parse(file.getName()).after(date);
                } catch (ParseException e) {
                    return true;
                }
            });
            if (files != null) for (var dir : files) {
                File[] listFiles = dir.listFiles();
                if (listFiles != null) for (var file : listFiles) file.delete();
                dir.delete();
            }
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage.", e);
        }
    }
}
