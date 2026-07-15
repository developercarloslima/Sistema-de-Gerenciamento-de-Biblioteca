package com.carlos.library.controller;

import com.carlos.library.domain.enums.BookAssetType;
import com.carlos.library.dto.ApiDtos.*;
import com.carlos.library.service.DigitalBookService;
import com.carlos.library.service.ReadingProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/books/{bookId}")
@RequiredArgsConstructor
public class DigitalBookController {
    private final DigitalBookService digitalBooks;
    private final ReadingProgressService progress;

    @PostMapping("/assets/upload-url")
    @PreAuthorize("hasRole('ADMIN')")
    AssetUploadResponse createUpload(@PathVariable UUID bookId,
                                     @Valid @RequestBody AssetUploadRequest request,
                                     Authentication authentication) {
        return digitalBooks.createUpload(bookId, request, authentication.getName());
    }

    @PostMapping("/assets/uploads/{sessionId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    BookAssetResponse confirm(@PathVariable UUID bookId, @PathVariable UUID sessionId,
                              Authentication authentication) {
        return digitalBooks.confirmUpload(bookId, sessionId, authentication.getName());
    }

    @GetMapping("/assets")
    List<BookAssetResponse> list(@PathVariable UUID bookId, Authentication authentication) {
        return digitalBooks.list(bookId, authentication.getName());
    }

    @GetMapping("/assets/{type}/access")
    AssetAccessResponse access(@PathVariable UUID bookId, @PathVariable BookAssetType type,
                               @RequestParam(defaultValue = "false") boolean download,
                               Authentication authentication) {
        return digitalBooks.access(bookId, type, download, authentication.getName());
    }

    @DeleteMapping("/assets/{type}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    void delete(@PathVariable UUID bookId, @PathVariable BookAssetType type) {
        digitalBooks.delete(bookId, type);
    }

    @GetMapping("/reading-progress")
    ReadingProgressResponse getProgress(@PathVariable UUID bookId, Authentication authentication) {
        return progress.get(bookId, authentication.getName());
    }

    @PutMapping("/reading-progress")
    ReadingProgressResponse updateProgress(@PathVariable UUID bookId,
                                           @Valid @RequestBody ReadingProgressRequest request,
                                           Authentication authentication) {
        return progress.update(bookId, request, authentication.getName());
    }
}
