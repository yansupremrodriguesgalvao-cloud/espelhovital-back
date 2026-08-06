package com.example.espelhovitalback.dto;

/**
 * Resposta padrão para endpoints que só precisam informar sucesso/erro com
 * uma mensagem — evita devolver texto puro (que quebra o `res.json()` do
 * front-end) em respostas de erro.
 */
public class MensagemDTO {

    private boolean sucesso;
    private String mensagem;

    public MensagemDTO() {
    }

    public MensagemDTO(boolean sucesso, String mensagem) {
        this.sucesso = sucesso;
        this.mensagem = mensagem;
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public void setSucesso(boolean sucesso) {
        this.sucesso = sucesso;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
