package com.example.espelhovitalback.service;

import com.example.espelhovitalback.config.CurrentUser;
import com.example.espelhovitalback.dto.*;
import com.example.espelhovitalback.model.Diario.Sintoma;
import com.example.espelhovitalback.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;

import java.awt.Color;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReportService {
    private final CurrentUser current;
    private final UsuarioRepository users;
    private final DiarioService diary;
    private final SonoRepository sleeps;
    private final HumorRepository moods;
    private final CicloMenstrualRepository cycles;
    private final ConsultaRepository appointments;
    private final Clock clock;

    public ReportService(CurrentUser current, UsuarioRepository users, DiarioService diary, SonoRepository sleeps, HumorRepository moods,
                         CicloMenstrualRepository cycles, ConsultaRepository appointments, Clock clock) {
        this.current = current;
        this.users = users;
        this.diary = diary;
        this.sleeps = sleeps;
        this.moods = moods;
        this.cycles = cycles;
        this.appointments = appointments;
        this.clock = clock;
    }

    public ReportData data(LocalDate start, LocalDate end, List<String> sections) {
        diary.validateRange(start, end);
        Set<String> chosen = new LinkedHashSet<>(sections);
        if (chosen.isEmpty() || !Set.of("DIARIO", "SONO", "HUMOR", "CICLO", "CONSULTAS").containsAll(chosen))
            throw new IllegalArgumentException("Escolha ao menos uma seção válida para o resumo.");
        Long id = current.id();
        var user = users.findById(id).orElseThrow();
        List<DiarioResponse> entries = new ArrayList<>();
        if (chosen.contains("DIARIO")) {
            int page = 0;
            boolean more;
            do {
                var batch = diary.history(start, end, page++, 100);
                entries.addAll(batch.registros().stream().map(r -> new DiarioResponse(r.data(), r.registrado(), r.fluxo(), r.intensidadeDor(), r.sintomas(), chosen.contains("HUMOR") ? r.humor() : null, chosen.contains("SONO") ? r.horasSono() : null, chosen.contains("SONO") ? r.qualidadeSono() : null, r.observacoes(), r.atualizadoEm())).toList());
                more = batch.temProxima();
            } while (more);
            entries.sort(Comparator.comparing(DiarioResponse::data));
        }
        return new ReportData(user.getNome(), start, end, clock.instant(), entries,
                chosen.contains("SONO") ? sleeps.findByUsuarioIdAndDataRegistroBetweenOrderByDataRegistroAscIdAsc(id, start, end) : List.of(),
                chosen.contains("HUMOR") ? moods.findByUsuarioIdAndDataRegistroBetweenOrderByDataRegistroAscIdAsc(id, start, end) : List.of(),
                chosen.contains("CICLO") ? cycles.findByUsuarioIdAndDataUltimaMenstruacaoBetweenOrderByDataUltimaMenstruacaoAscIdAsc(id, start, end) : List.of(),
                chosen.contains("CONSULTAS") ? appointments.findByUsuarioIdAndDataAgendadaBetweenOrderByDataAgendadaAscIdAsc(id, start, end) : List.of(), List.copyOf(chosen));
    }

    public byte[] pdf(ReportData data) throws IOException {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream(); var writer = new PdfWriter(document)) {
            document.getDocumentInformation().setTitle("Espelho Vital - Resumo para consulta");
            writer.heading("Espelho Vital", 20);
            writer.heading("Resumo para consulta", 15);
            writer.line("Nome: " + data.nome());
            writer.line("Período: " + date(data.inicio()) + " a " + date(data.fim()));
            writer.line("Gerado em: " + date(LocalDate.ofInstant(data.geradoEm(), clock.getZone())));
            writer.line("Registros pessoais para apoiar a conversa na consulta. Não constitui diagnóstico.");
            writer.line("Somente seções escolhidas e registros do período. Previsões de ciclo não são registros confirmados.");
            if (data.secoes().contains("DIARIO")) {
                writer.section("Diário integrado", data.diario().size());
                for (var r : data.diario()) {
                    writer.heading(date(r.data()), 11);
                    writer.line("Fluxo: " + r.fluxo().name().replace('_', ' ') + " | Dor: " + (r.intensidadeDor() == null ? "não informada" : r.intensidadeDor() + "/10"));
                    if (!r.sintomas().isEmpty())
                        writer.line("Sintomas: " + String.join(", ", r.sintomas().stream().map(Sintoma::descricao).toList()));
                    if (r.humor() != null) writer.line("Humor: " + r.humor());
                    if (r.horasSono() != null) writer.line("Sono: " + r.horasSono() + " h | " + r.qualidadeSono());
                    if (r.observacoes() != null) writer.line("Observações: " + r.observacoes());
                }
            }
            if (data.secoes().contains("SONO")) {
                writer.section("Sono", data.sono().size());
                for (var r : data.sono())
                    writer.line(date(r.getDataRegistro()) + " | " + r.getHoras() + " h | " + r.getQualidade());
            }
            if (data.secoes().contains("HUMOR")) {
                writer.section("Humor", data.humor().size());
                for (var r : data.humor()) {
                    writer.line(date(r.getDataRegistro()) + " | " + r.getEstado());
                    if (r.getObservacao() != null) writer.line("Observações: " + r.getObservacao());
                }
            }
            if (data.secoes().contains("CICLO")) {
                writer.section("Inícios menstruais registrados", data.ciclos().size());
                for (var r : data.ciclos())
                    writer.line(date(r.getDataUltimaMenstruacao()) + " | Sangramento informado: " + r.getDuracaoMenstruacao() + " dias | Ciclo informado como " + (Boolean.TRUE.equals(r.getIrregular()) ? "irregular" : "regular"));
                for (int i = 1; i < data.ciclos().size(); i++) {
                    var a = data.ciclos().get(i - 1);
                    var b = data.ciclos().get(i);
                    writer.line("Intervalo observado de " + date(a.getDataUltimaMenstruacao()) + " a " + date(b.getDataUltimaMenstruacao()) + ": " + java.time.temporal.ChronoUnit.DAYS.between(a.getDataUltimaMenstruacao(), b.getDataUltimaMenstruacao()) + " dias.");
                }
            }
            if (data.secoes().contains("CONSULTAS")) {
                writer.section("Consultas e exames", data.consultas().size());
                for (var r : data.consultas()) {
                    writer.line(date(r.getDataAgendada()) + (r.getHoraAgendada() == null ? "" : " " + r.getHoraAgendada()) + " | " + r.getTitulo() + " | " + r.getStatus());
                    if (r.getProfissional() != null) writer.line("Profissional: " + r.getProfissional());
                    if (r.getLocal() != null) writer.line("Local: " + r.getLocal());
                    if (r.getObservacao() != null) writer.line("Observações: " + r.getObservacao());
                }
            }
            writer.finish();
            document.save(output);
            return output.toByteArray();
        }
    }

    private String date(LocalDate date) {
        return date == null ? "Sem data" : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static class PdfWriter implements AutoCloseable {
        private final PDDocument document;
        private final PDType0Font font;
        private PDPageContentStream stream;
        private float y;
        private int page;

        PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            try (var input = ReportService.class.getResourceAsStream("/fonts/NotoSans-Regular.ttf")) {
                if (input == null) throw new IOException("Fonte do relatório indisponível.");
                font = PDType0Font.load(document, input);
            }
            newPage();
        }

        private void newPage() throws IOException {
            if (stream != null) {
                footer();
                stream.close();
            }
            PDPage sheet = new PDPage(PDRectangle.A4);
            document.addPage(sheet);
            stream = new PDPageContentStream(document, sheet);
            page++;
            y = 785;
        }

        private void footer() throws IOException {
            stream.beginText();
            stream.setFont(font, 8);
            stream.setNonStrokingColor(new Color(120, 105, 139));
            stream.newLineAtOffset(45, 30);
            stream.showText("Espelho Vital - uso pessoal | Página " + page);
            stream.endText();
        }

        void heading(String value, int size) throws IOException {
            y -= 12;
            write(value, size, true);
            y -= 5;
        }

        void section(String title, int count) throws IOException {
            heading(title + " (" + count + ")", 13);
            if (count == 0) line("Nenhum registro neste período.");
        }

        void line(String value) throws IOException {
            for (String part : value.split("\\R", -1)) write(part, 10, false);
        }

        private String safe(String text) throws IOException {
            StringBuilder result = new StringBuilder();
            for (int point : text.codePoints().toArray())
                if (point >= 32 && font.hasGlyph(point)) result.appendCodePoint(point);
            return result.toString();
        }

        private void write(String value, int size, boolean title) throws IOException {
            String text = safe(value);
            StringBuilder line = new StringBuilder();
            // Character wrapping also handles unusually long unbroken notes safely.
            for (int point : text.codePoints().toArray()) {
                String next = new String(Character.toChars(point));
                if (font.getStringWidth(line + next) / 1000 * size > 505 && !line.isEmpty()) {
                    draw(line.toString(), size, title);
                    line.setLength(0);
                }
                line.append(next);
            }
            draw(line.toString(), size, title);
        }

        private void draw(String text, int size, boolean title) throws IOException {
            if (y < 65) newPage();
            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(title ? new Color(86, 43, 135) : new Color(55, 47, 64));
            stream.newLineAtOffset(45, y);
            stream.showText(text);
            stream.endText();
            y -= size + 6;
        }

        void finish() throws IOException {
            if (stream != null) {
                footer();
                stream.close();
                stream = null;
            }
        }

        public void close() throws IOException {
            finish();
        }
    }
}
