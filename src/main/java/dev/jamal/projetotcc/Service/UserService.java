package dev.jamal.projetotcc.Service;

import dev.jamal.projetotcc.DTO.User.UserCreateRequestDTO;
import dev.jamal.projetotcc.DTO.User.UserResponseDTO;
import dev.jamal.projetotcc.Entities.User;
import dev.jamal.projetotcc.Exception.BusinessException;
import dev.jamal.projetotcc.Exception.ResourceNotFoundException;
import dev.jamal.projetotcc.Mapper.UserMapper;
import dev.jamal.projetotcc.Repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import dev.jamal.projetotcc.Enum.Role;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicy;


    // CREATE
    public UserResponseDTO cadastrar(UserCreateRequestDTO dto){

        if (userRepository.existsByEmail(dto.getEmail())){
            throw new BusinessException("Email já cadastrado.");
        }

        passwordPolicy.validar(dto.getSenha());
        User user = userMapper.toEntity(dto);
        user.setRole(Role.USER);
        user.setSenha(passwordEncoder.encode(dto.getSenha()));
        User salvo= userRepository.save(user);


        return userMapper.toResponseDTO(salvo);
    }

    // READ ALL
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public List<UserResponseDTO> listarTodos(){
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    // READ BY ID
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or #p0 == authentication.principal.id")
    public UserResponseDTO buscarPorId(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        return userMapper.toResponseDTO(user);
    }

    // UPDATE
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or #p0 == authentication.principal.id")
    public UserResponseDTO atualizar(
        Long id,
        UserCreateRequestDTO dto
    ) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // valida email duplicado
        userRepository.findByEmail(dto.getEmail())
                .ifPresent(outro -> {
                    if (!outro.getId().equals(id)) {
                        throw new BusinessException("Email já está em uso.");
                    }
                });

        passwordPolicy.validar(dto.getSenha());
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setSenha(passwordEncoder.encode(dto.getSenha()));
        user.setDataNascimento(dto.getDataNascimento());

        User atualizado = userRepository.save(user);

        return userMapper.toResponseDTO(atualizado);
    }

    // DELETE
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or #p0 == authentication.principal.id")
    public void deletar(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado.");
        }

        userRepository.deleteById(id);
    }

    // Esse método pode existir para uso interno de outros services.
    //  Mas controller não deve chamar ele diretamente.
    @Transactional
    public User buscarEntidadePorId(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }
}
