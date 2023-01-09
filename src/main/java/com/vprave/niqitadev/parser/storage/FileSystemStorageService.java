package com.vprave.niqitadev.parser.storage;

import com.vprave.niqitadev.parser.parse.IllegalDocumentException;
import com.vprave.niqitadev.parser.parse.Utils;
import com.vprave.niqitadev.parser.parse.consumers.NbkiConsumer;
import com.vprave.niqitadev.parser.parse.consumers.OkbConsumer;
import com.vprave.niqitadev.parser.table.Result;
import com.vprave.niqitadev.parser.table.Row;
import io.github.jonathanlink.PDFLayoutTextStripper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.vprave.niqitadev.parser.parse.IllegalDocumentException.IllegalCause.*;

@Service
public class FileSystemStorageService {

    private final Path rootLocation;
    private final Logger logger = LogManager.getRootLogger();

    @Autowired
    public FileSystemStorageService(StorageProperties properties) {
        rootLocation = Paths.get(properties.getLocation());
    }

    public Result handle(MultipartFile file) throws IllegalDocumentException {
        PDDocument doc = null;
        try {
            if (file.isEmpty()) throw new StorageException("Failed to store empty file.");
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) throw new StorageException("Failed to store \"null\" file.");
            Path destinationFile = rootLocation.resolve(Paths.get(originalFilename)).normalize().getParent().toAbsolutePath();
            if (!destinationFile.equals(rootLocation.toAbsolutePath()))
                throw new StorageException("Cannot store file outside current directory.");
            byte[] bytes = file.getBytes();
            if (bytes.length < 65537) throw new IllegalDocumentException(TOO_SHORT, null);
            Path path = Path.of(destinationFile.toFile() + File.separator + System.currentTimeMillis()).normalize().toAbsolutePath();
            Files.createDirectory(path);
            File copied = new File(path.toFile(), originalFilename);
            com.google.common.io.Files.write(bytes, copied);

            logger.info("Starting detect from " + copied.getName());
            double l = System.nanoTime();
            RandomAccessFile raf = new RandomAccessFile(copied, "r");
            PDFParser pdfParser = new PDFParser(raf);
            pdfParser.parse();
            raf.close();

            Result result = get(doc = new PDDocument(pdfParser.getDocument()));
            logger.info("Parsing took " + (((double) System.nanoTime() - l) / 1000000000) + "s");
            return result;
        } catch (Exception e) {
            if (doc != null) try {
                doc.close();
            } catch (IOException ex) {
                throw new IllegalDocumentException(INVALID, ex);
            }
            throw new IllegalDocumentException(INCORRECT_FORMAT, e);
        } finally {
            System.gc();
        }
    }

    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }

    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }
}
