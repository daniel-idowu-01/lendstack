package ng.lendstack.common.api;

import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

/** Pagination wrapper used by every list endpoint. */
@Getter
@AllArgsConstructor
public class PageResponse<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final long totalItems;
    private final int totalPages;

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
            page.getContent().stream().map(mapper).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages());
    }
}
