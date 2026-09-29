package com.valencmz.fintrack.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.valencmz.fintrack.enums.AccountType;
import com.valencmz.fintrack.enums.CategoryType;
import com.valencmz.fintrack.enums.Rol;
import com.valencmz.fintrack.model.entity.Account;
import com.valencmz.fintrack.model.entity.Category;
import com.valencmz.fintrack.model.entity.User;

import jakarta.persistence.EntityManager;

/**
 * Corre contra PostgreSQL real. El punto es owner nullable: se resolvio con un
 * ALTER TABLE manual y no con una migracion, asi que si alguien reconstruye la
 * base el NOT NULL vuelve sin avisar y este test lo detecta.
 *
 * H2 no sirve para esto: no replica los constraints de la misma forma, y un test
 * que pasa en H2 y falla en produccion no prueba nada.
 */
@DataJpaTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Repositorios contra PostgreSQL real")
class AccountRepositoryIT {

    /**
     * Arranca en un bloque estatico y no con @Container: @DataJpaTest crea el
     * contexto de Spring, y la conexion se resuelve antes de que la extension de
     * Testcontainers llegue a levantar el contenedor.
     */
    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @AfterAll
    static void stopPostgres() {
        POSTGRES.stop();
    }

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    private User user() {
        User u = new User(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID() + "@test.com",
                "hash",
                Rol.USER);
        return userRepository.saveAndFlush(u);
    }

    private Account account(User user, String name, boolean active) {
        Account a = new Account();
        a.setName(name);
        a.setType(AccountType.BANK);
        a.setActive(active);
        a.setUser(user);
        return accountRepository.saveAndFlush(a);
    }

    @Test
    @DisplayName("owner puede ser null: la columna no es NOT NULL")
    void ownerPuedeSerNull() {
        User user = user();
        Account a = account(user, "Sin titular", true);
        entityManager.clear();

        Account reloaded = accountRepository.findById(a.getId()).orElseThrow();

        assertThat(reloaded.getOwner()).isNull();
    }

    @Test
    @DisplayName("owner se guarda cuando viene")
    void ownerSeGuarda() {
        User user = user();
        Account a = new Account();
        a.setName("Con titular");
        a.setType(AccountType.BANK);
        a.setOwner("Valentin");
        a.setActive(true);
        a.setUser(user);
        a = accountRepository.saveAndFlush(a);
        entityManager.clear();

        assertThat(accountRepository.findById(a.getId()).orElseThrow().getOwner()).isEqualTo("Valentin");
    }

    @Test
    @DisplayName("findByUserIdAndActive excluye las inactivas")
    void elListadoExcluyeLasInactivas() {
        User user = user();
        account(user, "Activa", true);
        account(user, "Desactivada", false);

        List<Account> activas = accountRepository.findByUserIdAndActive(user.getId(), true);

        assertThat(activas).hasSize(1);
        assertThat(activas.get(0).getName()).isEqualTo("Activa");
    }

    @Test
    @DisplayName("findByUserIdAndActive no mezcla usuarios")
    void elListadoNoMezclaUsuarios() {
        User mine = user();
        User other = user();
        account(mine, "Mia", true);
        account(other, "Ajena", true);

        List<Account> mias = accountRepository.findByUserIdAndActive(mine.getId(), true);

        assertThat(mias).hasSize(1);
        assertThat(mias.get(0).getName()).isEqualTo("Mia");
    }

    @Test
    @DisplayName("el filtro de categorías también excluye las inactivas")
    void elFiltroDeCategoriasExcluyeLasInactivas() {
        User user = user();

        Category activa = new Category();
        activa.setName("Alquiler");
        activa.setColor("#FF0000");
        activa.setType(CategoryType.EXPENSE);
        activa.setActive(true);
        activa.setUser(user);
        categoryRepository.saveAndFlush(activa);

        Category inactiva = new Category();
        inactiva.setName("Baja");
        inactiva.setColor("#00FF00");
        inactiva.setType(CategoryType.EXPENSE);
        inactiva.setActive(false);
        inactiva.setUser(user);
        categoryRepository.saveAndFlush(inactiva);

        assertThat(categoryRepository.findByUserIdAndActive(user.getId(), true))
                .hasSize(1)
                .allSatisfy(c -> assertThat(c.isActive()).isTrue());
    }

    @Test
    @DisplayName("un email duplicado viola el UNIQUE de la base")
    void emailDuplicadoViolaElUnico() {
        User first = user();

        User duplicate = new User(
                "otro" + UUID.randomUUID().toString().substring(0, 6),
                first.getEmail(),
                "hash",
                Rol.USER);

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(duplicate);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
