package com.example.espelhovitalback.dto;

public class FaceStatusDTO {

    private boolean cadastrado;

    public FaceStatusDTO() {
    }

    public FaceStatusDTO(boolean cadastrado) {
        this.cadastrado = cadastrado;
    }

    public boolean isCadastrado() {
        return cadastrado;
    }

    public void setCadastrado(boolean cadastrado) {
        this.cadastrado = cadastrado;
    }
}
