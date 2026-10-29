package com.example.espelhovitalback.service;

import com.example.espelhovitalback.dto.PerfilDTO;
import com.example.espelhovitalback.dto.UsuarioDTO;
import com.example.espelhovitalback.model.Usuario;
import com.example.espelhovitalback.repository.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioService {
    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder, com.example.espelhovitalback.config.CurrentUser current) {
        this.current = current;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }


    private final com.example.espelhovitalback.config.CurrentUser current;
    private final UsuarioRepository repository;

    private final PasswordEncoder passwordEncoder;

    public void cadastrar(UsuarioDTO dto) {

        if (repository.findByEmailIgnoreCase(dto.getEmail().trim()).isPresent()) {
            throw new IllegalArgumentException("Já existe uma usuária cadastrada com este e-mail.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));

        repository.save(usuario);
    }

    /**
     * Verifica as credenciais e retorna a usuária autenticada, se válidas.
     */
    public Optional<Usuario> login(String email, String senha) {

        Optional<Usuario> usuarioOpt = repository.findByEmailIgnoreCase(email.trim());

        if (usuarioOpt.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuario = usuarioOpt.get();

        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            return Optional.empty();
        }

        return Optional.of(usuario);
    }

    public PerfilDTO buscarPerfil(Long id) {
        current.requireOwner(id);

        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuária não encontrada."));

        return toPerfilDTO(usuario);
    }

    public PerfilDTO atualizarPerfil(Long id, PerfilDTO dto) {
        current.requireOwner(id);

        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuária não encontrada."));

        if (dto.getNome() != null) usuario.setNome(dto.getNome());
        if (dto.getEmail() != null) {
            repository.findByEmailIgnoreCase(dto.getEmail().trim()).filter(u -> !u.getId().equals(id))
                    .ifPresent(u -> {
                        throw new IllegalArgumentException("E-mail já cadastrado.");
                    });
        }
        if (dto.getEmail() != null) usuario.setEmail(dto.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
        if (dto.getTelefone() != null) usuario.setTelefone(dto.getTelefone());
        if (dto.getDataNascimento() != null) usuario.setDataNascimento(dto.getDataNascimento());
        if (dto.getCidade() != null) usuario.setCidade(dto.getCidade());
        if (dto.getPeso() != null) usuario.setPeso(dto.getPeso());
        if (dto.getAltura() != null) usuario.setAltura(dto.getAltura());
        if (dto.getObjetivo() != null) usuario.setObjetivo(dto.getObjetivo());

        repository.save(usuario);

        return toPerfilDTO(usuario);
    }

    // ==================== RECONHECIMENTO FACIAL ====================

    /**
     * Distância euclidiana máxima entre descritores para considerar a mesma pessoa (padrão usado pelo face-api.js).
     */
    private static final double LIMIAR_DISTANCIA = 0.5;

    /**
     * Salva/atualiza o descritor facial (128 números gerados pelo face-api.js
     * no navegador) associado à usuária, para uso futuro no login facial.
     */
    public void cadastrarDescritorFacial(Long usuarioId, List<Double> descritor) {
        current.requireOwner(usuarioId);

        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuária não encontrada."));

        if (descritor == null || descritor.size() != 128 || descritor.stream().anyMatch(v -> v == null || !Double.isFinite(v))) {
            throw new IllegalArgumentException("Descritor facial inválido.");
        }

        String texto = descritor.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        usuario.setFaceDescriptor(texto);
        repository.save(usuario);
    }

    /**
     * Compara o descritor apenas com a conta confirmada por e-mail e senha.
     * O vetor recebido do navegador não comprova uma captura ao vivo.
     */
    public Optional<Usuario> loginPorReconhecimentoFacial(String email, String senha, List<Double> descritor) {
        if (descritor == null || descritor.size() != 128 || descritor.stream().anyMatch(v -> v == null || !Double.isFinite(v)))
            return Optional.empty();
        return login(email, senha).filter(user -> {
            if (user.getFaceDescriptor() == null || user.getFaceDescriptor().isBlank()) return false;
            try {
                List<Double> saved = Arrays.stream(user.getFaceDescriptor().split(",")).map(Double::parseDouble).toList();
                return saved.size() == 128 && distanciaEuclidiana(descritor, saved) <= LIMIAR_DISTANCIA;
            } catch (NumberFormatException ex) {
                return false;
            }
        });
    }

    private double distanciaEuclidiana(List<Double> a, List<Double> b) {
        double soma = 0;
        for (int i = 0; i < a.size(); i++) {
            double diferenca = a.get(i) - b.get(i);
            soma += diferenca * diferenca;
        }
        return Math.sqrt(soma);
    }

    public void removerFace() {
        Usuario user = repository.findById(current.id()).orElseThrow();
        user.setFaceDescriptor(null);
        repository.save(user);
    }

    private PerfilDTO toPerfilDTO(Usuario usuario) {
        return new PerfilDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getDataNascimento(),
                usuario.getCidade(),
                usuario.getPeso(),
                usuario.getAltura(),
                usuario.getObjetivo()
        );
    }
}
