package ng.lendstack.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.audit.AuditService;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.document.dto.DocumentResponse;
import ng.lendstack.domain.Collateral;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.LoanDocument;
import ng.lendstack.repository.CollateralRepository;
import ng.lendstack.repository.LoanDocumentRepository;
import ng.lendstack.repository.LoanRepository;
import ng.lendstack.repository.UserRepository;
import ng.lendstack.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> DOC_TYPES = Set.of(
        "ID_CARD", "PAYSLIP", "BANK_STATEMENT", "UTILITY_BILL", "COLLATERAL_DOC", "OTHER");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf", "image/jpeg", "image/png");

    private final LoanDocumentRepository documentRepository;
    private final LoanRepository loanRepository;
    private final CollateralRepository collateralRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final AuditService auditService;

    @Transactional
    public DocumentResponse upload(UUID uploaderId, UUID loanId, String docType,
                                   UUID collateralId, MultipartFile file) {
        Loan loan = ownedLoan(uploaderId, loanId);
        if (!DOC_TYPES.contains(docType)) {
            throw ApiException.badRequest("INVALID_DOC_TYPE",
                "docType must be one of " + DOC_TYPES);
        }
        if (file.isEmpty()) {
            throw ApiException.badRequest("EMPTY_FILE", "The uploaded file is empty");
        }
        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw ApiException.badRequest("UNSUPPORTED_FILE_TYPE",
                "Only PDF, JPEG and PNG documents are accepted");
        }
        Collateral collateral = null;
        if (collateralId != null) {
            collateral = collateralRepository.findById(collateralId)
                .orElseThrow(() -> ApiException.notFound("Collateral record not found"));
            if (!collateral.getLoan().getId().equals(loanId)) {
                throw ApiException.badRequest("COLLATERAL_MISMATCH",
                    "That collateral record belongs to a different loan");
            }
        }
        String storagePath;
        try (InputStream in = file.getInputStream()) {
            storagePath = storageService.store("loans/" + loanId, safeName(file), in);
        } catch (IOException e) {
            throw new IllegalStateException("Upload failed", e);
        }
        LoanDocument document = documentRepository.save(LoanDocument.builder()
            .loan(loan)
            .collateral(collateral)
            .uploadedBy(userRepository.getReferenceById(uploaderId))
            .docType(docType)
            .fileName(safeName(file))
            .contentType(file.getContentType())
            .storagePath(storagePath)
            .sizeBytes(file.getSize())
            .build());
        auditService.record("LOAN", loanId.toString(), "DOCUMENT_UPLOADED",
            null, Map.of("documentId", document.getId().toString(), "docType", docType,
                "fileName", document.getFileName()), null);
        return DocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> forOwnLoan(UUID borrowerId, UUID loanId) {
        ownedLoan(borrowerId, loanId);
        return forLoan(loanId);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> forLoan(UUID loanId) {
        return documentRepository.findByLoanId(loanId).stream()
            .map(DocumentResponse::from).toList();
    }

    public record DocumentStream(LoanDocument document, InputStream content) {
    }


    @Transactional(readOnly = true)
    public DocumentStream downloadOwn(UUID borrowerId, UUID documentId) {
        LoanDocument document = get(documentId);
        if (!document.getLoan().getBorrower().getId().equals(borrowerId)) {
            throw ApiException.notFound("Document not found");
        }
        return new DocumentStream(document, storageService.retrieve(document.getStoragePath()));
    }


    @Transactional(readOnly = true)
    public DocumentStream download(UUID documentId) {
        LoanDocument document = get(documentId);
        return new DocumentStream(document, storageService.retrieve(document.getStoragePath()));
    }

    private LoanDocument get(UUID documentId) {
        return documentRepository.findById(documentId)
            .orElseThrow(() -> ApiException.notFound("Document not found"));
    }

    private String safeName(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private Loan ownedLoan(UUID borrowerId, UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> ApiException.notFound("Loan not found"));
        if (!loan.getBorrower().getId().equals(borrowerId)) {
            throw ApiException.notFound("Loan not found");
        }
        return loan;
    }
}
