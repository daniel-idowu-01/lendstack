package ng.lendstack.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.config.dto.ConfigResponse;
import ng.lendstack.config.dto.ConfigUpdateRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin — System configuration",
    description = "Runtime CBN/lending parameters: interest rate cap, tenure bounds, penalty "
        + "rate, grace period, collateral threshold, guarantor tiers. The database is the source "
        + "of truth — when the CBN revises a guideline, ADMIN updates it here with no redeploy. "
        + "Every change is audit-logged with old and new values.")
@RestController
@RequestMapping("/api/v1/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final SystemConfigService configService;

    @Operation(summary = "All configuration entries")
    @GetMapping
    public ApiResponse<List<ConfigResponse>> all() {
        return ApiResponse.ok(configService.all().stream().map(ConfigResponse::from).toList());
    }

    @Operation(summary = "Update a configuration value",
        description = "Type-checked against the entry's value_type. Applies immediately to all "
            + "new decisions (existing loans keep the terms they were approved with).")
    @PutMapping("/{key}")
    public ApiResponse<ConfigResponse> update(@PathVariable String key,
                                              @Valid @RequestBody ConfigUpdateRequest request) {
        return ApiResponse.ok(ConfigResponse.from(configService.update(key, request.value())),
            "Configuration updated");
    }
}
