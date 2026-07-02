package ng.lendstack.document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.document.dto.DocumentResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Officer — Documents",
    description = "Read-only access to borrower-uploaded documents for application review and "
        + "collateral verification.")
@RestController
@RequestMapping("/api/v1/officer")
@RequiredArgsConstructor
public class OfficerDocumentController {

    private final DocumentService documentService;

    @Operation(summary = "List a loan's documents")
    @GetMapping("/loans/{loanId}/documents")
    public ApiResponse<List<DocumentResponse>> list(@PathVariable UUID loanId) {
        return ApiResponse.ok(documentService.forLoan(loanId));
    }

    @Operation(summary = "Download a document")
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID documentId) {
        var stream = documentService.download(documentId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + stream.document().getFileName() + "\"")
            .contentType(MediaType.parseMediaType(stream.document().getContentType()))
            .body(new InputStreamResource(stream.content()));
    }
}
