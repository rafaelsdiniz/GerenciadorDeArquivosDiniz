package diniz.contabilidade.arquivos.resource;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import diniz.contabilidade.arquivos.dto.response.ArquivoResponseDTO;
import diniz.contabilidade.arquivos.dto.response.DocumentoAnalisado;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoPendente;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
import diniz.contabilidade.arquivos.model.enums.TipoDocumento;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import diniz.contabilidade.arquivos.repository.ObrigacaoPendenteRepository;
import diniz.contabilidade.arquivos.security.UsuarioLogado;
import diniz.contabilidade.arquivos.service.ArquivoService;
import diniz.contabilidade.arquivos.service.ia.AnalisadorDocumento;
import diniz.contabilidade.arquivos.service.ia.GeradorExemplos;
import diniz.contabilidade.arquivos.service.ia.LeitorDocumentoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Leitura inteligente de documentos (IA DeepSeek + leitura por padrões).
 *
 *   GET  /ia/status                      → IA configurada? modelo
 *   POST /ia/analisar                    → lê um arquivo enviado (multipart) SEM gravar — usado antes do envio
 *   POST /ia/arquivos/{id}/analisar      → lê um arquivo armazenado e grava os dados extraídos
 *   GET  /ia/exemplos/{nome}             → guias fictícias (DAS/FGTS) para demonstração
 *
 * Isolamento: o cliente (FUNCIONARIO) só lê documentos da própria empresa (404 para as demais).
 */
@Path("/ia")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "FUNCIONARIO"})
public class IaResource {

    private static final Logger LOG = Logger.getLogger(IaResource.class);
    private static final long MAX_BYTES = 15L * 1024 * 1024;

    @Inject
    LeitorDocumentoService leitor;

    @Inject
    diniz.contabilidade.arquivos.repository.ArquivoRepository arquivoRepository;

    @Inject
    ArquivoService arquivoService;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    ObrigacaoPendenteRepository obrigacaoRepository;

    @Inject
    UsuarioLogado usuario;

    public record StatusIa(boolean configurada, String provedor, String modelo, boolean leituraPorPadroes) {}

    public record ResultadoArquivo(DocumentoAnalisado analise, ArquivoResponseDTO arquivo) {}

    @GET
    @Path("/status")
    public StatusIa status() {
        return new StatusIa(leitor.configurada(), "DeepSeek", leitor.configurada() ? leitor.modelo() : null, true);
    }

    @POST
    @Path("/analisar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public DocumentoAnalisado analisar(@RestForm("arquivo") FileUpload arquivo,
                                       @RestForm("idObrigacaoPendente") Long idObrigacaoPendente,
                                       @RestForm("idEmpresa") Long idEmpresa) throws java.io.IOException {
        if (arquivo == null || arquivo.uploadedFile() == null) {
            throw new IllegalArgumentException("Envie o arquivo a ser lido.");
        }
        if (Files.size(arquivo.uploadedFile()) > MAX_BYTES) {
            throw new IllegalArgumentException("Arquivo grande demais para a leitura inteligente (máx. 15 MB).");
        }

        ObrigacaoPendente obrigacao = null;
        if (idObrigacaoPendente != null) {
            obrigacao = obrigacaoRepository.findByIdOptional(idObrigacaoPendente)
                    .orElseThrow(() -> new NotFoundException("Obrigação não encontrada."));
            usuario.exigirEmpresa(obrigacao.getEmpresa().getId());
        }
        Empresa empresa = null;
        if (idEmpresa != null) {
            usuario.exigirEmpresa(idEmpresa);
            empresa = empresaRepository.findByIdOptional(idEmpresa)
                    .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));
        } else if (obrigacao == null && !usuario.isAdmin() && usuario.empresaId() != null) {
            empresa = empresaRepository.findById(usuario.empresaId());
        }
        if (empresa != null && obrigacao != null && !empresa.getId().equals(obrigacao.getEmpresa().getId())) {
            throw new IllegalArgumentException("A obrigação não pertence à empresa informada.");
        }

        byte[] bytes = Files.readAllBytes(arquivo.uploadedFile());
        return leitor.analisar(bytes, arquivo.fileName(), empresa, obrigacao);
    }

    @POST
    @Path("/arquivos/{idArquivo}/analisar")
    public ResultadoArquivo analisarArquivo(@PathParam("idArquivo") Long idArquivo) {
        ArquivoResponseDTO a = usuario.verificar(arquivoService.buscarPorId(idArquivo), ArquivoResponseDTO::idEmpresa);
        if (a.excluidoEm() != null) {
            throw new IllegalArgumentException("Arquivo está na lixeira; restaure antes de lê-lo.");
        }
        DocumentoAnalisado r = leitor.analisarArquivo(idArquivo);
        // a leitura gravou em outra transação: descarta a cópia carregada antes para devolver os dados novos
        arquivoRepository.getEntityManager().clear();
        return new ResultadoArquivo(r, arquivoService.buscarPorId(idArquivo));
    }

    // ------------------------------------------------------------------ exemplos para demonstração

    /**
     * Gera uma guia fictícia em PDF (com texto e linha digitável válida) para testar a leitura.
     * Usa a empresa/obrigação informadas (ou a do usuário) para que a guia "bata" com uma obrigação real do sistema.
     */
    @GET
    @Path("/exemplos/{nome}")
    @Produces("application/pdf")
    public Response exemplo(@PathParam("nome") String nome,
                            @QueryParam("idObrigacaoPendente") Long idObrigacaoPendente,
                            @QueryParam("idEmpresa") Long idEmpresa) {
        String n = nome == null ? "" : nome.toLowerCase();
        TipoDocumento tipo = n.contains("fgts") ? TipoDocumento.FGTS : n.contains("das") ? TipoDocumento.DAS : null;
        if (tipo == null) throw new NotFoundException("Exemplo não encontrado. Disponíveis: " + GeradorExemplos.DAS + ", " + GeradorExemplos.FGTS);

        ObrigacaoPendente obrigacao = null;
        Empresa empresa;
        if (idObrigacaoPendente != null) {
            obrigacao = obrigacaoRepository.findByIdOptional(idObrigacaoPendente)
                    .orElseThrow(() -> new NotFoundException("Obrigação não encontrada."));
            usuario.exigirEmpresa(obrigacao.getEmpresa().getId());
            empresa = obrigacao.getEmpresa();
        } else {
            Long id = idEmpresa != null ? idEmpresa : usuario.empresaId();
            if (id == null) throw new NotFoundException("Empresa não encontrada.");
            usuario.exigirEmpresa(id);
            empresa = empresaRepository.findByIdOptional(id).orElseThrow(() -> new NotFoundException("Empresa não encontrada."));
            obrigacao = obrigacaoParecida(empresa, tipo);
        }

        LocalDate vencimento;
        if (obrigacao != null && obrigacao.getDataVencimento() != null) {
            vencimento = obrigacao.getDataVencimento();
        } else {
            LocalDate hoje = LocalDate.now();
            int dia = 20;
            vencimento = hoje.getDayOfMonth() <= dia ? hoje.withDayOfMonth(dia) : hoje.plusMonths(1).withDayOfMonth(dia);
        }
        YearMonth competencia = YearMonth.from(vencimento).minusMonths(1);
        String razao = empresa.getRazaoSocial() != null ? empresa.getRazaoSocial() : empresa.getNomeFantasia();
        String cnpj = empresa.getCnpj() != null ? empresa.getCnpj().getNumero() : "00000000000000";

        GeradorExemplos.Dados dados = new GeradorExemplos.Dados(razao, cnpj, competencia, vencimento,
                tipo == TipoDocumento.FGTS ? new BigDecimal("1076.00") : new BigDecimal("1847.32"));
        try {
            GeradorExemplos g = new GeradorExemplos();
            byte[] pdf = tipo == TipoDocumento.FGTS ? g.fgts(dados) : g.das(dados);
            String arquivo = tipo == TipoDocumento.FGTS ? GeradorExemplos.FGTS : GeradorExemplos.DAS;
            return Response.ok(pdf, "application/pdf")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo + "\"")
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao gerar guia de exemplo", e);
            return Response.serverError().type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("codigo", "EXEMPLO", "mensagem", "Não foi possível gerar o exemplo.")).build();
        }
    }

    /** Obrigação da empresa do mesmo tipo: a pendente mais próxima; senão, a de vencimento mais próximo de hoje. */
    private ObrigacaoPendente obrigacaoParecida(Empresa empresa, TipoDocumento tipo) {
        List<ObrigacaoPendente> todas = obrigacaoRepository.buscarPorEmpresa(empresa).stream()
                .filter(o -> o.getObrigacaoRecorrente() != null && o.getDataVencimento() != null
                        && AnalisadorDocumento.tiposEsperados(o.getObrigacaoRecorrente().getNome()).contains(tipo))
                .toList();
        LocalDate hoje = LocalDate.now();
        return todas.stream()
                .filter(o -> o.getStatus() != StatusObrigacao.ENTREGUE)
                .min(Comparator.comparing((ObrigacaoPendente o) -> Math.abs(o.getDataVencimento().toEpochDay() - hoje.toEpochDay())))
                .orElseGet(() -> todas.stream()
                        .min(Comparator.comparing((ObrigacaoPendente o) -> Math.abs(o.getDataVencimento().toEpochDay() - hoje.toEpochDay())))
                        .orElse(null));
    }
}
