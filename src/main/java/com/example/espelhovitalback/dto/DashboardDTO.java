package com.example.espelhovitalback.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Consolida os principais indicadores de saúde da usuária em um único payload,
 * usado pela tela "Dashboard de Acompanhamento da Saúde".
 */
public class DashboardDTO {

    // Ciclo menstrual
    private LocalDate proximaMenstruacao;
    private LocalDate ovulacao;
    private LocalDate inicioPeriodoFertil;
    private LocalDate fimPeriodoFertil;
    private Integer diasParaProximaMenstruacao;
    private String faseCicloAtual;

    // Sono
    private Double mediaHorasSono;
    private String ultimaQualidadeSono;
    private Integer totalRegistrosSono;

    // Humor
    private String ultimoHumor;
    private LocalDate dataUltimoHumor;
    private String humorPredominante;
    private String analiseHumor;

    // Consultas / exames
    private Integer totalConsultasProximas;
    private List<ConsultaResumoDTO> proximasConsultas;

    // Alertas / lembretes reunidos para exibir no topo do dashboard
    private List<String> alertas;

    public LocalDate getProximaMenstruacao() {
        return proximaMenstruacao;
    }

    public void setProximaMenstruacao(LocalDate proximaMenstruacao) {
        this.proximaMenstruacao = proximaMenstruacao;
    }

    public LocalDate getOvulacao() {
        return ovulacao;
    }

    public void setOvulacao(LocalDate ovulacao) {
        this.ovulacao = ovulacao;
    }

    public LocalDate getInicioPeriodoFertil() {
        return inicioPeriodoFertil;
    }

    public void setInicioPeriodoFertil(LocalDate inicioPeriodoFertil) {
        this.inicioPeriodoFertil = inicioPeriodoFertil;
    }

    public LocalDate getFimPeriodoFertil() {
        return fimPeriodoFertil;
    }

    public void setFimPeriodoFertil(LocalDate fimPeriodoFertil) {
        this.fimPeriodoFertil = fimPeriodoFertil;
    }

    public Integer getDiasParaProximaMenstruacao() {
        return diasParaProximaMenstruacao;
    }

    public void setDiasParaProximaMenstruacao(Integer diasParaProximaMenstruacao) {
        this.diasParaProximaMenstruacao = diasParaProximaMenstruacao;
    }

    public String getFaseCicloAtual() {
        return faseCicloAtual;
    }

    public void setFaseCicloAtual(String faseCicloAtual) {
        this.faseCicloAtual = faseCicloAtual;
    }

    public Double getMediaHorasSono() {
        return mediaHorasSono;
    }

    public void setMediaHorasSono(Double mediaHorasSono) {
        this.mediaHorasSono = mediaHorasSono;
    }

    public String getUltimaQualidadeSono() {
        return ultimaQualidadeSono;
    }

    public void setUltimaQualidadeSono(String ultimaQualidadeSono) {
        this.ultimaQualidadeSono = ultimaQualidadeSono;
    }

    public Integer getTotalRegistrosSono() {
        return totalRegistrosSono;
    }

    public void setTotalRegistrosSono(Integer totalRegistrosSono) {
        this.totalRegistrosSono = totalRegistrosSono;
    }

    public String getUltimoHumor() {
        return ultimoHumor;
    }

    public void setUltimoHumor(String ultimoHumor) {
        this.ultimoHumor = ultimoHumor;
    }

    public LocalDate getDataUltimoHumor() {
        return dataUltimoHumor;
    }

    public void setDataUltimoHumor(LocalDate dataUltimoHumor) {
        this.dataUltimoHumor = dataUltimoHumor;
    }

    public String getHumorPredominante() {
        return humorPredominante;
    }

    public void setHumorPredominante(String humorPredominante) {
        this.humorPredominante = humorPredominante;
    }

    public String getAnaliseHumor() {
        return analiseHumor;
    }

    public void setAnaliseHumor(String analiseHumor) {
        this.analiseHumor = analiseHumor;
    }

    public Integer getTotalConsultasProximas() {
        return totalConsultasProximas;
    }

    public void setTotalConsultasProximas(Integer totalConsultasProximas) {
        this.totalConsultasProximas = totalConsultasProximas;
    }

    public List<ConsultaResumoDTO> getProximasConsultas() {
        return proximasConsultas;
    }

    public void setProximasConsultas(List<ConsultaResumoDTO> proximasConsultas) {
        this.proximasConsultas = proximasConsultas;
    }

    public List<String> getAlertas() {
        return alertas;
    }

    public void setAlertas(List<String> alertas) {
        this.alertas = alertas;
    }
}
