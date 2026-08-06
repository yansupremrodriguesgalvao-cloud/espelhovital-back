package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.dto.FaceDescriptorRequest;
import com.example.espelhovitalback.dto.FaceStatusDTO;
import com.example.espelhovitalback.dto.LoginResponse;
import com.example.espelhovitalback.dto.MensagemDTO;
import com.example.espelhovitalback.dto.PerfilDTO;
import com.example.espelhovitalback.dto.UsuarioDTO;
import com.example.espelhovitalback.model.Usuario;
import com.example.espelhovitalback.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin("*")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<String> cadastrar(@Valid @RequestBody UsuarioDTO dto) {
        try {
            service.cadastrar(dto);
            return ResponseEntity.ok("Usuária cadastrada com sucesso!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPerfil(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.buscarPerfil(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarPerfil(@PathVariable Long id, @RequestBody PerfilDTO dto) {
        try {
            return ResponseEntity.ok(service.atualizarPerfil(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // ==================== RECONHECIMENTO FACIAL ====================

    /** Informa se a usuária já tem um rosto cadastrado (para o front ajustar a tela). */
    @GetMapping("/{id}/face/status")
    public ResponseEntity<?> statusFace(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(new FaceStatusDTO(service.possuiFaceCadastrada(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(new MensagemDTO(false, e.getMessage()));
        }
    }

    /** Cadastra/atualiza o "rosto" (descritor facial) da usuária já logada. */
    @PostMapping("/face/cadastrar")
    public ResponseEntity<MensagemDTO> cadastrarFace(@RequestBody FaceDescriptorRequest request) {
        try {
            service.cadastrarDescritorFacial(request.getUsuarioId(), request.getDescritor());
            return ResponseEntity.ok(new MensagemDTO(true, "Rosto cadastrado com sucesso!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MensagemDTO(false, e.getMessage()));
        }
    }

    /** Login usando apenas o rosto (compara com os descritores cadastrados). */
    @PostMapping("/face/login")
    public ResponseEntity<?> loginFacial(@RequestBody FaceDescriptorRequest request) {

        Optional<Usuario> usuarioOpt = service.loginPorReconhecimentoFacial(request.getDescritor());

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            return ResponseEntity.ok(new LoginResponse(
                    usuario.getId(),
                    usuario.getNome(),
                    usuario.getEmail(),
                    "Login facial realizado com sucesso!"
            ));
        }

        return ResponseEntity.status(401)
                .body(new MensagemDTO(false, "Rosto não reconhecido. Tente novamente ou use e-mail e senha."));
    }
}
