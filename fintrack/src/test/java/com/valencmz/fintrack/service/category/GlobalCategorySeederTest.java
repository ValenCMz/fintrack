package com.valencmz.fintrack.service.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import com.valencmz.fintrack.model.entity.Category;
import com.valencmz.fintrack.repository.CategoryRepository;

@DisplayName("Semilla de categorías globales por defecto")
class GlobalCategorySeederTest {

    private CategoryRepository categoryRepository;
    private GlobalCategorySeeder seeder;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        seeder = new GlobalCategorySeeder();
        ReflectionTestUtils.setField(seeder, "categoryRepository", categoryRepository);
    }

    @Test
    @DisplayName("si no existen globales, inserta el set por defecto sin dueño")
    @SuppressWarnings("unchecked")
    void insertaDefaultsCuandoNoHayGlobales() {
        when(categoryRepository.existsByUserIsNull()).thenReturn(false);

        seeder.run(mock(ApplicationArguments.class));

        verify(categoryRepository).saveAll(org.mockito.ArgumentMatchers.argThat(list -> {
            List<Category> categories = (List<Category>) list;
            return categories.size() == 14
                    && categories.stream().allMatch(c -> c.getUser() == null && c.isActive());
        }));
    }

    @Test
    @DisplayName("si ya existen globales, no hace nada (idempotente)")
    void noHaceNadaSiYaHayGlobales() {
        when(categoryRepository.existsByUserIsNull()).thenReturn(true);

        seeder.run(mock(ApplicationArguments.class));

        verify(categoryRepository, never()).saveAll(anyList());
        verify(categoryRepository, never()).save(any());
    }
}
