package com.example.controller;

import com.example.domain.UploadedDocument;
import com.example.service.DocumentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/document")
public class DocumentController {

    @Autowired
    private DocumentService documentService;
    /**
     *
     * @param files
     * @return  mao1.txt 上传成功 \n mao2.txt 上传成功 \n
     */
    @PostMapping("/upload")
    public Mono<ResponseEntity<String>> upload(@RequestParam("file") List<MultipartFile> files,
                                                HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Mono.just(new ResponseEntity<>(HttpStatusCode.valueOf(401)));
        }
        Mono<String> result = documentService.upload(files, userId);
        Mono<ResponseEntity<String>> ret = result
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatusCode.valueOf(401)));

        return ret;
    }

    @GetMapping("/list")
    public Mono<List<UploadedDocument>> list(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Mono.just(List.of());
        }
        return documentService.listUploadedDocuments(userId);
    }
}
