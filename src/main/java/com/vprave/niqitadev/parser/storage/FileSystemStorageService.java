package com.vprave.niqitadev.parser.storage;

import com.vprave.niqitadev.parser.parse.IllegalDocumentException;
import com.vprave.niqitadev.parser.parse.Parser;
import com.vprave.niqitadev.parser.table.Result;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.vprave.niqitadev.parser.parse.IllegalDocumentException.IllegalCause.INCORRECT_FORMAT;
import static com.vprave.niqitadev.parser.parse.IllegalDocumentException.IllegalCause.INVALID;

@Service
public class FileSystemStorageService {

    private final Path rootLocation;
    private final Logger logger = LogManager.getRootLogger();

    @Autowired
    public FileSystemStorageService(StorageProperties properties) {
        rootLocation = properties.getLocation();
    }

    public Result handleFile(MultipartFile file) throws IllegalDocumentException {
        PDDocument doc = null;
        try {
            if (file.isEmpty()) throw new StorageException("Failed to store empty file.");
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) throw new StorageException("Failed to store file with wrong name.");
            Path path = rootLocation.resolve(Long.toString(System.currentTimeMillis())).normalize().toAbsolutePath();
            Files.createDirectories(path);
            File copied = new File(path.toFile(), originalFilename);
            file.transferTo(copied);

            logger.info("Starting detect from " + copied.getAbsolutePath());
            double l = System.nanoTime();
            RandomAccessFile raf = new RandomAccessFile(copied, "r");
            PDFParser pdfParser = new PDFParser(raf);
            pdfParser.parse();
            raf.close();

            Result result = new Parser().get(doc = new PDDocument(pdfParser.getDocument()));
            logger.info("Parsing took " + (((double) System.nanoTime() - l) / 1000000000) + "s");
            return result;
        } catch (Exception e) {
            logger.error("Something went wrong", e);
            try {
                doc.close();
            } catch (Exception exception) {
                throw new IllegalDocumentException(INVALID, exception);
            }
            throw new IllegalDocumentException(INCORRECT_FORMAT, e);
        } finally {
            System.gc();
        }
    }

}
