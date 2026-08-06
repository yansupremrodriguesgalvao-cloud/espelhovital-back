package com.example.espelhovitalback.service;

import com.example.espelhovitalback.dto.PerfilDTO;
import com.example.espelhovitalback.dto.UsuarioDTO;
import com.example.espelhovitalback.model.Usuario;
import com.example.espelhovitalback.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void cadastrar(UsuarioDTO dto) {

        if (repository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Já existe uma usuária cadastrada com este e-mail.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));

        repository.save(usuario);
    }

    /**
     * Verifica as credenciais e retorna a usuária autenticada, se válidas.
     */
    public Optional<Usuario> login(String email, String senha) {

        Optional<Usuario> usuarioOpt = repository.findByEmail(email);

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

        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuária não encontrada."));

        return toPerfilDTO(usuario);
    }

    public PerfilDTO atualizarPerfil(Long id, PerfilDTO dto) {

        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuária não encontrada."));

        if (dto.getNome() != null) usuario.setNome(dto.getNome());
        if (dto.getEmail() != null) usuario.setEmail(dto.getEmail());
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

    /** Distância euclidiana máxima entre descritores para considerar a mesma pessoa (padrão usado pelo face-api.js). */
    private static final double LIMIAR_DISTANCIA = 0.5;

    /**
     * Salva/atualiza o descritor facial (128 números gerados pelo face-api.js
     * no navegador) associado à usuária, para uso futuro no login facial.
     */
    public void cadastrarDescritorFacial(Long usuarioId, List<Double> descritor) {

        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuária não encontrada."));

        if (descritor == null || descritor.size() != 128) {
            throw new IllegalArgumentException("Descritor facial inválido.");
        }

        String texto = descritor.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        usuario.setFaceDescriptor(texto);
        repository.save(usuario);
    }

    /** Indica se a usuária já possui um rosto cadastrado para login facial. */
    public boolean possuiFaceCadastrada(Long usuarioId) {

        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuária não encontrada."));

        return usuario.getFaceDescriptor() != null && !usuario.getFaceDescriptor().isBlank();
    }

    /**
     * Compara o descritor recebido da câmera com os descritores salvos de
     * todas as usuárias e retorna a que mais se parece, se estiver dentro do
     * limiar de similaridade.
     */
    public Optional<Usuario> loginPorReconhecimentoFacial(List<Double> descritorRecebido) {

        if (descritorRecebido == null || descritorRecebido.size() != 128) {
            return Optional.empty();
        }

        Usuario melhorCandidata = null;
        double menorDistancia = Double.MAX_VALUE;

        for (Usuario usuario : repository.findAll()) {

            if (usuario.getFaceDescriptor() == null || usuario.getFaceDescriptor().isBlank()) {
                continue;
            }

            List<Double> salvo = Arrays.stream(usuario.getFaceDescriptor().split(","))
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());

            double distancia = distanciaEuclidiana(descritorRecebido, salvo);

            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                melhorCandidata = usuario;
            }
        }

        if (melhorCandidata != null && menorDistancia <= LIMIAR_DISTANCIA) {
            return Optional.of(melhorCandidata);
        }

        return Optional.empty();
    }

    private double distanciaEuclidiana(List<Double> a, List<Double> b) {
        double soma = 0;
        for (int i = 0; i < a.size(); i++) {
            double diferenca = a.get(i) - b.get(i);
            soma += diferenca * diferenca;
        }
        return Math.sqrt(soma);
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