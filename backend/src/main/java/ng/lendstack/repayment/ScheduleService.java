package ng.lendstack.repayment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.RepaymentInstallment;
import ng.lendstack.finance.Amortization;
import ng.lendstack.repository.RepaymentInstallmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class ScheduleService {

    static final ZoneId LAGOS = ZoneId.of("Africa/Lagos");

    private final RepaymentInstallmentRepository installmentRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public List<RepaymentInstallment> generate(Loan loan) {
        BigDecimal principal = loan.getAmount();
        BigDecimal rate = loan.getInterestRateAnnual();
        int months = loan.getTenureMonths();
        BigDecimal payment = Amortization.monthlyPayment(principal, rate, months);
        LocalDate firstDue = LocalDate.now(LAGOS).plusMonths(1);

        List<RepaymentInstallment> schedule = new ArrayList<>(months);
        BigDecimal outstanding = principal;
        for (int i = 1; i <= months; i++) {
            BigDecimal interest = Amortization.interestOn(outstanding, rate);
            BigDecimal principalPart = i == months
                ? outstanding                       // final installment clears the balance exactly
                : payment.subtract(interest);
            schedule.add(installmentRepository.save(RepaymentInstallment.builder()
                .loan(loan)
                .installmentNumber(i)
                .dueDate(firstDue.plusMonths(i - 1L))
                .principalDue(principalPart)
                .interestDue(interest)
                .totalDue(principalPart.add(interest))
                .build()));
            outstanding = outstanding.subtract(principalPart);
        }
        return schedule;
    }
}
