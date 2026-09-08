package com.example.espelhovitalback.dto;

import java.util.List;

public class FaceDescriptorRequest {

    private Long usuarioId;
    private List<Double> descritor;

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public List<Double> getDescritor() {
        return descritor;
    }

    public void setDescritor(List<Double> descritor) {
        this.descritor = descritor;
    }
}
