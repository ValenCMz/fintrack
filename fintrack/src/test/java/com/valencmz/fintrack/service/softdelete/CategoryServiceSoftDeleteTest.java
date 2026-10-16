package com.valencmz.fintrack.service.softdelete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.valencmz.fintrack.enums.CategoryType;
import com.valencmz.fintrack.enums.Rol;
import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.dto.category.CategoryRequest;
import com.valencmz.fintrack.model.entity.Category;
import com.valencmz.fintrack.model.entity.User;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.CategoryRepository;
import com.valencmz.fintrack.service.category.CategoryService;

@DisplayName("Categorías: baja lógica y defaults globales")
class CategoryServiceSoftDeleteTest {

    private CategoryRepository categoryRepository;
    private CategoryService categoryService;
    private UserAuth userAuth;
    private UUID userId;

    @BeforeEach
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

        when(categoryRepository.findAvailableForUser(any())).thenReturn(List.of());
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

    private Category globalCategory(String name) {
        Category c = new Category();
        c.setId(UUID.randomUUID());
        c.setName(name);
        c.setColor("#EF4444");
        c.setType(CategoryType.EXPENSE);
        c.setActive(true);
        c.setUser(null);
        return c;
    }

    private CategoryRequest request() {
        return new CategoryRequest("editada", "#123456", CategoryType.EXPENSE, true);
    }

    @Test
    @DisplayName("desactiva la categoría en vez de borrarla")
    void desactivaEnVezDeBorrar() {
        Category existing = category("Alquiler", true);
        when(categoryRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        categoryService.softDelete(existing.getId(), userAuth);

        assertThat(existing.isActive()).isFalse();
        verify(categoryRepository).save(existing);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("el listado devuelve las globales y las propias activas")
    void elListadoDevuelveGlobalesYPropias() {
        Category global = globalCategory("Supermercado");
        Category propia = category("Alquiler", true);
        when(categoryRepository.findAvailableForUser(userId)).thenReturn(List.of(global, propia));

        List<com.valencmz.fintrack.model.dto.category.CategoryResponse> result = categoryService.getByUser(userAuth);

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(r -> assertThat(r.isGlobal()).isTrue());
        verify(categoryRepository).findAvailableForUser(userId);
    }

    @Test
    @DisplayName("el detalle sigue devolviendo la categoría desactivada")
    void elDetalleSigueDevolviendoLaDesactivada() {
        Category deactivated = category("Alquiler", false);
        when(categoryRepository.findVisibleById(deactivated.getId(), userId))
                .thenReturn(Optional.of(deactivated));

        assertThat(categoryService.getById(deactivated.getId(), userAuth).isActive()).isFalse();
    }

    @Test
    @DisplayName("el detalle de una categoría global funciona y se marca como global")
    void elDetalleDeUnaGlobal() {
        Category global = globalCategory("Transporte");
        when(categoryRepository.findVisibleById(global.getId(), userId)).thenReturn(Optional.of(global));

        assertThat(categoryService.getById(global.getId(), userAuth).isGlobal()).isTrue();
    }

    @Test
    @DisplayName("borrar una categoría ajena da 404")
    void borrarCategoriaAjenaDa404() {
        User otro = new User();
        otro.setId(UUID.randomUUID());
        Category ajena = category("Ajena", true);
        ajena.setUser(otro);
        when(categoryRepository.findById(ajena.getId())).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> categoryService.softDelete(ajena.getId(), userAuth))
                .isInstanceOf(CustomAppException.class)
                .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("borrar una categoría inexistente da 404")
    void borrarCategoriaInexistenteDa404() {
        when(categoryRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.softDelete(UUID.randomUUID(), userAuth))
                .isInstanceOf(CustomAppException.class)
                .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("no se puede borrar una categoría global (403)")
    void noSePuedeBorrarUnaGlobal() {
        Category global = globalCategory("Supermercado");
        when(categoryRepository.findById(global.getId())).thenReturn(Optional.of(global));

        assertThatThrownBy(() -> categoryService.softDelete(global.getId(), userAuth))
                .isInstanceOf(CustomAppException.class)
                .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("editar una categoría global da 403")
    void editarCategoriaGlobalDa403() {
        Category global = globalCategory("Supermercado");
        when(categoryRepository.findById(global.getId())).thenReturn(Optional.of(global));

        assertThatThrownBy(() -> categoryService.update(global.getId(), request(), userAuth))
                .isInstanceOf(CustomAppException.class)
                .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("la categoría creada queda asignada al usuario (nunca global)")
    void laCategoriaCreadaQuedaAsignadaAlUsuario() {
        CategoryRequest request = request();

        categoryService.create(request, userAuth);

        verify(categoryRepository).save(org.mockito.ArgumentMatchers.argThat(
                c -> c.getUser() != null && c.getUser().getId().equals(userId)));
    }
}
