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


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collateral_id")
    private Collateral collateral;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;


    @Column(name = "doc_type", nullable = false)
    private String docType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;


    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
}
