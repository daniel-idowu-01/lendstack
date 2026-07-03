package ng.lendstack.document.dto;

import java.time.Instant;
import ng.lendstack.domain.LoanDocument;


public record DocumentResponse(
    String id,
    String docType,
    String fileName,
    String contentType,
    long sizeBytes,
    String collateralId,
    String uploadedBy,
    Instant uploadedAt
) {

    public static DocumentResponse from(LoanDocument d) {
        return new DocumentResponse(d.getId().toString(), d.getDocType(), d.getFileName(),
            d.getContentType(), d.getSizeBytes(),
            d.getCollateral() == null ? null : d.getCollateral().getId().toString(),
            d.getUploadedBy().getEmail(), d.getCreatedAt());
    }
}
