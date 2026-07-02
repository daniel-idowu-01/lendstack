package ng.lendstack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Uploaded supporting document (ID card, payslip, bank statement, collateral papers). */
@Entity
@Table(name = "loan_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    /** Set when the document belongs to a collateral record. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collateral_id")
    private Collateral collateral;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    /** ID_CARD | PAYSLIP | BANK_STATEMENT | UTILITY_BILL | COLLATERAL_DOC | OTHER */
    @Column(name = "doc_type", nullable = false)
    private String docType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    /** Path under the document storage root (never exposed raw to clients). */
    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
}
