package com.vprave.niqitadev.parser.parse;

import com.vprave.niqitadev.parser.storage.FileSystemStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class FileUploadController {
    private final FileSystemStorageService storageService;
    @Autowired
    public FileUploadController(FileSystemStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/")
    public String showStartPage() {
        return "index";
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        try {
            model.addAttribute("table", storageService.handleFile(file));
        } catch (IllegalDocumentException e) {
            model.addAttribute("err", e.getMessage());
        }
        return "index";
    }
}
