package com.valencmz.fintrack.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.valencmz.fintrack.enums.CategoryType;
import com.valencmz.fintrack.model.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsByUserIsNull();

    // Categorías activas visibles para un usuario: las globales (user is null) más
    // las propias.
    @Query("select c from Category c where c.active = true and (c.user.id = :userId or c.user is null) order by c.type, c.name")
    List<Category> findAvailableForUser(@Param("userId") UUID userId);

    // Resuelve una categoría visible para el usuario (propia o global).
    @Query("select c from Category c where c.id = :id and (c.user.id = :userId or c.user is null)")
    Optional<Category> findVisibleById(@Param("id") UUID id, @Param("userId") UUID userId);

    Optional<Category> findFirstByNameAndUserIsNullAndType(String name, CategoryType type);
}
