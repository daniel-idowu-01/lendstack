package ng.lendstack.document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.document.dto.DocumentResponse;
import ng.lendstack.security.UserPrincipal;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Borrower — Documents",
    description = "Supporting documents: ID card, payslip, bank statement, utility bill, "
        + "collateral papers. PDF/JPEG/PNG up to 10MB. Attach a collateralId to link a document "
        + "to a specific collateral record for officer verification.")
@RestController
@RequestMapping("/api/v1/borrower")
@RequiredArgsConstructor
public class BorrowerDocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Upload a document",
        description = "Multipart upload. docType: ID_CARD | PAYSLIP | BANK_STATEMENT | "
            + "UTILITY_BILL | COLLATERAL_DOC | OTHER.")
    @PostMapping(value = "/loans/{loanId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID loanId,
            @RequestParam String docType,
            @RequestParam(required = false) UUID collateralId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
            documentService.upload(principal.getId(), loanId, docType, collateralId, file),
            "Document uploaded"));
    }

    @Operation(summary = "List a loan's documents")
    @GetMapping("/loans/{loanId}/documents")
    public ApiResponse<List<DocumentResponse>> list(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable UUID loanId) {
        return ApiResponse.ok(documentService.forOwnLoan(principal.getId(), loanId));
    }

    @Operation(summary = "Download one of my documents")
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<InputStreamResource> download(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID documentId) {
        var stream = documentService.downloadOwn(principal.getId(), documentId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + stream.document().getFileName() + "\"")
            .contentType(MediaType.parseMediaType(stream.document().getContentType()))
            .body(new InputStreamResource(stream.content()));
    }
}
