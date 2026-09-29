package com.valencmz.fintrack.service.softdelete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.valencmz.fintrack.enums.CategoryType;
import com.valencmz.fintrack.enums.Rol;
import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.entity.Category;
import com.valencmz.fintrack.model.entity.User;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.CategoryRepository;
import com.valencmz.fintrack.service.category.CategoryService;

@DisplayName("Categorías: baja lógica")
class CategoryServiceSoftDeleteTest {

    private CategoryRepository categoryRepository;
    private CategoryService categoryService;
    private UserAuth userAuth;
    private UUID userId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        categoryService = new CategoryService();
        ReflectionTestUtils.setField(categoryService, "categoryRepository", categoryRepository);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setRol(Rol.USER);
        userId = user.getId();
        userAuth = new UserAuth(user);

        when(categoryRepository.findByUserIdAndActive(any(), anyBoolean())).thenReturn(List.of());
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Category category(String name, boolean active) {
        Category c = new Category();
        c.setId(UUID.randomUUID());
        c.setName(name);
        c.setColor("#FF0000");
        c.setType(CategoryType.EXPENSE);
        c.setActive(active);
        c.setUser(userAuth.getUser());
        return c;
    }

    @Test
    @DisplayName("desactiva la categoría en vez de borrarla")
    void desactivaEnVezDeBorrar() {
        Category existing = category("Alquiler", true);
        when(categoryRepository.findByIdAndUserId(existing.getId(), userId))
                .thenReturn(Optional.of(existing));

        categoryService.softDelete(existing.getId(), userAuth);

        assertThat(existing.isActive()).isFalse();
        verify(categoryRepository).save(existing);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("el listado consulta solo las activas")
    void elListadoConsultaSoloLasActivas() {
        when(categoryRepository.findByUserIdAndActive(userId, true))
                .thenReturn(List.of(category("Alquiler", true)));

        categoryService.getByUser(userAuth);

        verify(categoryRepository).findByUserIdAndActive(userId, true);
        verify(categoryRepository, never()).findByUserIdAndActive(userId, false);
    }

    @Test
    @DisplayName("el detalle sigue devolviendo la categoría desactivada")
    void elDetalleSigueDevolviendoLaDesactivada() {
        Category deactivated = category("Alquiler", false);
        when(categoryRepository.findByIdAndUserId(deactivated.getId(), userId))
                .thenReturn(Optional.of(deactivated));

        assertThat(categoryService.getById(deactivated.getId(), userAuth).isActive()).isFalse();
    }

    @Test
    @DisplayName("borrar una categoría ajena da 404")
    void borrarCategoriaAjenaDa404() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.softDelete(id, userAuth))
                .isInstanceOf(CustomAppException.class)
                .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(categoryRepository, never()).save(any());
    }
}
