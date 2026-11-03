package dev.jamal.projetotcc.security;

import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.DTO.User.UserCreateRequestDTO;
import dev.jamal.projetotcc.Repository.UserRepository;
import dev.jamal.projetotcc.Mapper.UserMapper;
import dev.jamal.projetotcc.Service.*;
import dev.jamal.projetotcc.Controllers.AuthController;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserSessionSecurityTest {
    @Test void passwordUpdateBumpsRevocationVersion() {
        var repo=mock(UserRepository.class);
        var encoder=mock(PasswordEncoder.class);
        var service=new UserService(repo,mock(UserMapper.class),encoder,new PasswordPolicyService());
        User user=new User(); user.setId(1L); user.setTokenVersion(7);
        when(repo.findById(1L)).thenReturn(Optional.of(user));
        when(repo.findByEmail("a@example.test")).thenReturn(Optional.of(user));
        var dto=new UserCreateRequestDTO(); dto.setNome("Teste"); dto.setEmail("a@example.test"); dto.setSenha("Ficticia!Com-Entropia-2026");
        service.atualizar(1L,dto);
        assertEquals(8,user.getTokenVersion()); verify(repo).save(user);
    }
    @Test void logoutUsesAtomicServerRevocation() {
        var repo=mock(UserRepository.class);
        var controller=new AuthController(mock(AuthService.class),repo);
        User user=new User(); user.setId(1L);
        assertEquals(204,controller.logout(user).getStatusCode().value());
        verify(repo).revokeTokens(1L);
    }
}
