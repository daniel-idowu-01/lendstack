package ng.lendstack.loan;

import ng.lendstack.domain.Loan;
import ng.lendstack.loan.dto.LoanResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface LoanMapper {

    @Mapping(target = "borrowerName", source = "borrower.fullName")
    LoanResponse toResponse(Loan loan);
}
