package ng.lendstack.config;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ng.lendstack.domain.BorrowerProfile;
import ng.lendstack.domain.Lender;
import ng.lendstack.domain.User;
import ng.lendstack.domain.enums.EmploymentStatus;
import ng.lendstack.domain.enums.LenderType;
import ng.lendstack.domain.enums.RiskTier;
import ng.lendstack.domain.enums.Role;
import ng.lendstack.repository.BorrowerProfileRepository;
import ng.lendstack.repository.LenderRepository;
import ng.lendstack.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Configuration
@Profile("dev")
@ConditionalOnProperty(name = "lendstack-dev.seed-demo-data", havingValue = "true")
@RequiredArgsConstructor
public class DevDataSeeder {

    private final UserRepository userRepository;
    private final BorrowerProfileRepository profileRepository;
    private final LenderRepository lenderRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner seedDemoData() {
        return args -> seed();
    }

    @Transactional
    void seed() {
        if (userRepository.existsByEmailIgnoreCase("admin@lendstack.ng")) {
            return;
        }
        userRepository.save(User.builder()
            .email("admin@lendstack.ng")
            .passwordHash(passwordEncoder.encode("Admin@1234"))
            .fullName("Adaeze Okafor")
            .phone("08010000001")
            .role(Role.ADMIN)
            .build());
        userRepository.save(User.builder()
            .email("officer@lendstack.ng")
            .passwordHash(passwordEncoder.encode("Officer@1234"))
            .fullName("Tunde Bakare")
            .phone("08010000002")
            .role(Role.LOAN_OFFICER)
            .build());
        User borrower = userRepository.save(User.builder()
            .email("amaka@example.com")
            .passwordHash(passwordEncoder.encode("Borrower@1234"))
            .fullName("Amaka Eze")
            .phone("08010000003")
            .role(Role.BORROWER)
            .build());
        profileRepository.save(BorrowerProfile.builder()
            .user(borrower)
            .bvn("22212345678")                 // stub NIBSS verifies BVNs not starting with 0
            .employmentStatus(EmploymentStatus.EMPLOYED)
            .employerName("Zenith Textiles Ltd")
            .monthlyIncome(new BigDecimal("450000"))
            .bankAccountNumber("0123456789")
            .bankName("GTBank")
            .address("14 Adeola Odeku St, Victoria Island, Lagos")
            .build());

        lenderRepository.save(Lender.builder()
            .name("Sterling Microcredit")
            .type(LenderType.INSTITUTION)
            .email("funding@sterlingmicro.ng")
            .walletBalance(new BigDecimal("20000000"))
            .maxExposure(new BigDecimal("50000000"))
            .preferredRiskTier(RiskTier.MEDIUM)
            .build());
        lenderRepository.save(Lender.builder()
            .name("Chinedu Nwosu")
            .type(LenderType.INDIVIDUAL)
            .email("chinedu.nwosu@example.com")
            .walletBalance(new BigDecimal("3000000"))
            .maxExposure(new BigDecimal("5000000"))
            .preferredRiskTier(RiskTier.LOW)
            .build());
        lenderRepository.save(Lender.builder()
            .name("Kudi Capital Partners")
            .type(LenderType.INSTITUTION)
            .email("desk@kudicapital.ng")
            .walletBalance(new BigDecimal("10000000"))
            .maxExposure(new BigDecimal("25000000"))
            .preferredRiskTier(RiskTier.HIGH)
            .build());
        log.info("Seeded dev demo data: 3 users, 3 lenders");
    }
}
