package diniz.contabilidade.arquivos.service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import diniz.contabilidade.arquivos.dto.request.ArquivoRequestDTO;
import diniz.contabilidade.arquivos.dto.request.CertidaoRequestDTO;
import diniz.contabilidade.arquivos.dto.response.ArquivoResponseDTO;
import diniz.contabilidade.arquivos.dto.response.CertidaoResponseDTO;
import diniz.contabilidade.arquivos.dto.response.CertidaoResumoDTO;
import diniz.contabilidade.arquivos.model.entity.Arquivo;
import diniz.contabilidade.arquivos.model.entity.Certidao;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.Pasta;
import diniz.contabilidade.arquivos.model.enums.AcaoLog;
import diniz.contabilidade.arquivos.model.enums.CategoriaFiscal;
import diniz.contabilidade.arquivos.repository.ArquivoRepository;
import diniz.contabilidade.arquivos.repository.CertidaoRepository;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import diniz.contabilidade.arquivos.repository.PastaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

/**
 * Certidões negativas de débitos com controle de validade.
 *
 * A consulta automática nos sites do governo fica fora do escopo (captcha / certificado digital):
 * o escritório emite a certidão, registra aqui (com o PDF no Drive) e o sistema avisa antes de vencer.
 */
@ApplicationScoped
public class CertidaoService {

    private static final String ENTIDADE = "Certidao";

    /** a partir de quantos dias para o vencimento a certidão passa a "vencendo" */
    public static final int DIAS_ALERTA = 15;

    public static final String VALIDA = "VALIDA";
    public static final String VENCENDO = "VENCENDO";
    public static final String VENCIDA = "VENCIDA";

    @Inject
    CertidaoRepository certidaoRepository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    ArquivoRepository arquivoRepository;

    @Inject
    PastaRepository pastaRepository;

    @Inject
    ArquivoService arquivoService;

    @Inject
    LogAcessoService logService;

    // ------------------------------------------------------------------ consultas

    public List<CertidaoResponseDTO> listar() {
        return certidaoRepository.listarTodas().stream().map(this::toResponseDTO).toList();
    }

    public List<CertidaoResponseDTO> listarPorEmpresa(Long idEmpresa) {
        empresaRepository.findByIdOptional(idEmpresa)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));
        return certidaoRepository.listarPorEmpresa(idEmpresa).stream().map(this::toResponseDTO).toList();
    }

    public CertidaoResponseDTO buscarPorId(Long id) {
        return toResponseDTO(buscarEntidade(id));
    }

    public List<Certidao> comValidadeEntre(Long idEmpresa, LocalDate inicio, LocalDate fim) {
        return certidaoRepository.buscarComValidadeEntre(idEmpresa, inicio, fim);
    }

    /** Resumo por situação de validade das certidões informadas (já filtradas por empresa). */
    public CertidaoResumoDTO resumo(List<CertidaoResponseDTO> certidoes) {
        long validas = certidoes.stream().filter(c -> VALIDA.equals(c.statusValidade())).count();
        long vencendo = certidoes.stream().filter(c -> VENCENDO.equals(c.statusValidade())).count();
        long vencidas = certidoes.stream().filter(c -> VENCIDA.equals(c.statusValidade())).count();
        long empresasComAlerta = certidoes.stream()
                .filter(c -> !VALIDA.equals(c.statusValidade()))
                .map(CertidaoResponseDTO::idEmpresa)
                .distinct()
                .count();
        Long proximo = certidoes.stream()
                .map(CertidaoResponseDTO::diasParaVencer)
                .filter(d -> d != null && d >= 0)
                .min(Comparator.naturalOrder())
                .orElse(null);
        return new CertidaoResumoDTO(certidoes.size(), validas, vencendo, vencidas, empresasComAlerta, proximo);
    }

    // ------------------------------------------------------------------ escrita

    @Transactional
    public CertidaoResponseDTO criar(CertidaoRequestDTO dto) {
        Certidao certidao = new Certidao();
        aplicar(certidao, dto);
        certidaoRepository.persist(certidao);
        logService.registrar(AcaoLog.ATUALIZAR_VENCIMENTO, ENTIDADE, certidao.getId(),
                "Certidão cadastrada: " + certidao.getTipo().getRotulo() + " válida até " + certidao.getDataValidade()
                        + " (empresa " + certidao.getEmpresa().getId() + ")");
        return toResponseDTO(certidao);
    }

    @Transactional
    public CertidaoResponseDTO atualizar(Long id, CertidaoRequestDTO dto) {
        Certidao certidao = buscarEntidade(id);
        LocalDate anterior = certidao.getDataValidade();
        aplicar(certidao, dto);
        logService.registrar(AcaoLog.ATUALIZAR_VENCIMENTO, ENTIDADE, id,
                "Certidão atualizada: " + certidao.getTipo().getRotulo() + " validade " + anterior + " -> " + certidao.getDataValidade());
        return toResponseDTO(certidao);
    }

    @Transactional
    public void excluir(Long id) {
        Certidao certidao = buscarEntidade(id);
        logService.registrar(AcaoLog.EXCLUIR, ENTIDADE, id,
                "Certidão excluída: " + certidao.getTipo().getRotulo() + " (empresa " + certidao.getEmpresa().getId() + ")");
        certidaoRepository.delete(certidao);
    }

    /**
     * Envia o PDF da certidão para o Drive da empresa (pasta "Certidões"; criada se não existir)
     * e vincula o arquivo à certidão.
     */
    @Transactional
    public CertidaoResponseDTO anexar(Long id, Long idUsuario, String nomeOriginal, long tamanho, String base64) {
        Certidao certidao = buscarEntidade(id);
        Empresa empresa = certidao.getEmpresa();
        Pasta pasta = pastaDeCertidoes(empresa);

        String descricao = certidao.getTipo().getRotulo()
                + (certidao.getNumero() != null && !certidao.getNumero().isBlank() ? " nº " + certidao.getNumero() : "")
                + " — válida até " + certidao.getDataValidade().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        if (descricao.length() > 250) descricao = descricao.substring(0, 250);

        ArquivoRequestDTO req = new ArquivoRequestDTO(
                empresa.getId(), idUsuario, pasta.getId(), descricao, null, null, CategoriaFiscal.CERTIDAO);
        ArquivoResponseDTO salvo = arquivoService.salvar(req, nomeOriginal, tamanho, base64);

        certidao.setArquivo(arquivoRepository.findById(salvo.id()));
        return toResponseDTO(certidao);
    }

    // ------------------------------------------------------------------ apoio

    public Certidao buscarEntidade(Long id) {
        return certidaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Certidão não encontrada."));
    }

    private void aplicar(Certidao c, CertidaoRequestDTO dto) {
        Empresa empresa = empresaRepository.findByIdOptional(dto.idEmpresa())
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        if (dto.dataEmissao() != null && dto.dataValidade() != null && dto.dataValidade().isBefore(dto.dataEmissao())) {
            throw new IllegalArgumentException("A validade não pode ser anterior à data de emissão.");
        }
        if (dto.dataEmissao() != null && dto.dataEmissao().isAfter(hoje())) {
            throw new IllegalArgumentException("A data de emissão não pode estar no futuro.");
        }

        // trocar a empresa de uma certidão com PDF anexado deixaria o arquivo no Drive de outra empresa
        if (c.getArquivo() != null && c.getEmpresa() != null && !Objects.equals(c.getEmpresa().getId(), empresa.getId())) {
            c.setArquivo(null);
        }

        c.setEmpresa(empresa);
        c.setTipo(dto.tipo());
        c.setSituacao(dto.situacao());
        c.setNumero(limpar(dto.numero()));
        c.setDataEmissao(dto.dataEmissao());
        c.setDataValidade(dto.dataValidade());
        c.setOrgaoEmissor(limpar(dto.orgaoEmissor()));
        c.setObservacao(limpar(dto.observacao()));

        if (dto.idArquivo() != null) {
            Arquivo arquivo = arquivoRepository.findByIdOptional(dto.idArquivo())
                    .orElseThrow(() -> new NotFoundException("Arquivo não encontrado."));
            if (!Objects.equals(arquivo.getEmpresa().getId(), empresa.getId())) {
                throw new IllegalArgumentException("O arquivo selecionado não pertence à empresa da certidão.");
            }
            c.setArquivo(arquivo);
        }
    }

    /** Pasta "Certidões" da empresa (prefere a de nível mais alto); cria na raiz se não houver. */
    private Pasta pastaDeCertidoes(Empresa empresa) {
        List<Pasta> pastas = pastaRepository.buscarPorEmpresa(empresa);
        Optional<Pasta> existente = pastas.stream()
                .filter(p -> normalizar(p.getNome()).startsWith("certid"))
                .min(Comparator.comparing((Pasta p) -> p.getPastaPai() == null ? 0 : 1).thenComparing(Pasta::getId));
        if (existente.isPresent()) return existente.get();

        Pasta nova = new Pasta();
        nova.setNome("Certidões");
        nova.setDescricao("Certidões negativas de débitos");
        nova.setEmpresa(empresa);
        pastaRepository.persist(nova);
        return nova;
    }

    private CertidaoResponseDTO toResponseDTO(Certidao c) {
        long dias = ChronoUnit.DAYS.between(hoje(), c.getDataValidade());
        Arquivo arq = c.getArquivo() != null && c.getArquivo().getExcluidoEm() == null ? c.getArquivo() : null;
        Empresa e = c.getEmpresa();
        return new CertidaoResponseDTO(
                c.getId(),
                e.getId(),
                e.getNomeFantasia() != null ? e.getNomeFantasia() : e.getRazaoSocial(),
                c.getTipo(),
                c.getTipo().getRotulo(),
                c.getSituacao(),
                c.getNumero(),
                c.getDataEmissao(),
                c.getDataValidade(),
                c.getOrgaoEmissor(),
                c.getObservacao(),
                arq != null ? arq.getId() : null,
                arq != null ? arq.getNomeOriginal() : null,
                statusValidade(dias),
                dias,
                c.getDataCriacao(),
                c.getDataAtualizacao()
        );
    }

    public static String statusValidade(long diasParaVencer) {
        if (diasParaVencer < 0) return VENCIDA;
        if (diasParaVencer <= DIAS_ALERTA) return VENCENDO;
        return VALIDA;
    }

    public static String statusValidade(LocalDate validade) {
        return statusValidade(ChronoUnit.DAYS.between(hoje(), validade));
    }

    private static LocalDate hoje() {
        return LocalDate.now();
    }

    private static String limpar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
    }
}
