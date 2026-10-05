package diniz.contabilidade.arquivos.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import diniz.contabilidade.arquivos.dto.request.MensagemRequestDTO;
import diniz.contabilidade.arquivos.dto.response.ConversaRecenteDTO;
import diniz.contabilidade.arquivos.dto.response.MensagemResponseDTO;
import diniz.contabilidade.arquivos.dto.response.MensagensNaoLidasDTO;
import diniz.contabilidade.arquivos.model.entity.Arquivo;
import diniz.contabilidade.arquivos.model.entity.MensagemObrigacao;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoPendente;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.model.enums.Periodicidade;
import diniz.contabilidade.arquivos.repository.ArquivoRepository;
import diniz.contabilidade.arquivos.repository.MensagemObrigacaoRepository;
import diniz.contabilidade.arquivos.repository.ObrigacaoPendenteRepository;
import diniz.contabilidade.arquivos.repository.UsuarioRepository;
import diniz.contabilidade.arquivos.security.UsuarioLogado;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

/**
 * Conversa entre escritório e cliente dentro de cada obrigação.
 *
 * O "lado" de quem consulta vem do perfil: ADMIN = escritório; FUNCIONARIO = cliente
 * (restrito à própria empresa; obrigações de outra empresa respondem 404).
 */
@ApplicationScoped
public class MensagemObrigacaoService {

    private static final int TAMANHO_PREVIA = 140;

    @Inject
    MensagemObrigacaoRepository repository;

    @Inject
    ObrigacaoPendenteRepository obrigacaoRepository;

    @Inject
    ArquivoRepository arquivoRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    UsuarioLogado usuario;

    /** Lista a conversa (mais antiga primeiro) e marca como lidas as mensagens do outro lado. */
    @Transactional
    public List<MensagemResponseDTO> abrirConversa(Long idObrigacao) {
        ObrigacaoPendente obrigacao = obrigacaoVisivel(idObrigacao);
        LocalDateTime agora = LocalDateTime.now();
        if (usuario.isAdmin()) {
            repository.marcarLidasPeloEscritorio(obrigacao.getId(), agora);
        } else {
            repository.marcarLidasPeloCliente(obrigacao.getId(), agora);
        }
        // o update em lote não atualiza entidades já carregadas: busca depois de marcar
        repository.getEntityManager().clear();
        Long eu = usuario.usuarioId();
        return repository.listarPorObrigacao(obrigacao.getId()).stream()
                .map(m -> toResponseDTO(m, eu))
                .toList();
    }

    @Transactional
    public MensagemResponseDTO enviar(Long idObrigacao, MensagemRequestDTO dto) {
        ObrigacaoPendente obrigacao = obrigacaoVisivel(idObrigacao);
        String texto = dto != null && dto.texto() != null ? dto.texto().trim() : "";
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("Escreva a mensagem antes de enviar.");
        }
        if (texto.length() > MensagemObrigacao.TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("A mensagem pode ter no máximo "
                    + MensagemObrigacao.TAMANHO_MAXIMO + " caracteres.");
        }

        Usuario autor = usuario.usuarioId() != null ? usuarioRepository.findById(usuario.usuarioId()) : null;
        if (autor == null) {
            throw new ForbiddenException("Usuário não identificado.");
        }

        Arquivo arquivo = null;
        if (dto.idArquivo() != null) {
            arquivo = arquivoRepository.findByIdOptional(dto.idArquivo())
                    .filter(a -> a.getExcluidoEm() == null)
                    .filter(a -> a.getEmpresa() != null && obrigacao.getEmpresa() != null
                            && Objects.equals(a.getEmpresa().getId(), obrigacao.getEmpresa().getId()))
                    .orElseThrow(() -> new IllegalArgumentException("Arquivo anexado inválido para esta obrigação."));
        }

        boolean doEscritorio = usuario.isAdmin();
        LocalDateTime agora = LocalDateTime.now();

        MensagemObrigacao m = new MensagemObrigacao();
        m.setObrigacaoPendente(obrigacao);
        m.setAutor(autor);
        m.setDoEscritorio(doEscritorio);
        m.setTexto(texto);
        m.setArquivo(arquivo);
        if (doEscritorio) {
            m.setLidaPeloEscritorioEm(agora);
            // responder também significa ter lido o que o cliente mandou antes
            repository.marcarLidasPeloEscritorio(obrigacao.getId(), agora);
        } else {
            m.setLidaPeloClienteEm(agora);
            repository.marcarLidasPeloCliente(obrigacao.getId(), agora);
        }
        repository.persist(m);
        return toResponseDTO(m, autor.getId());
    }

    public MensagensNaoLidasDTO naoLidas() {
        Map<Long, Long> porObrigacao = contagemNaoLidas();
        long total = porObrigacao.values().stream().mapToLong(Long::longValue).sum();
        return new MensagensNaoLidasDTO(total, porObrigacao);
    }

    public List<ConversaRecenteDTO> recentes(int limite) {
        int n = Math.max(1, Math.min(limite, 50));
        Long idEmpresa = usuario.isAdmin() ? null : usuario.empresaId();
        if (!usuario.isAdmin() && idEmpresa == null) {
            return List.of();
        }
        Map<Long, Long> naoLidas = contagemNaoLidas();
        Long eu = usuario.usuarioId();
        return repository.ultimasPorConversa(idEmpresa, n).stream()
                .map(m -> toConversaDTO(m, eu, naoLidas))
                .toList();
    }

    private Map<Long, Long> contagemNaoLidas() {
        if (usuario.isAdmin()) {
            return repository.naoLidasPorObrigacao(true, null);
        }
        Long idEmpresa = usuario.empresaId();
        if (idEmpresa == null) {
            return Map.of();
        }
        return repository.naoLidasPorObrigacao(false, idEmpresa);
    }

    private ObrigacaoPendente obrigacaoVisivel(Long idObrigacao) {
        ObrigacaoPendente obrigacao = obrigacaoRepository.findByIdOptional(idObrigacao)
                .orElseThrow(() -> new NotFoundException("Obrigação não encontrada."));
        usuario.exigirEmpresa(obrigacao.getEmpresa() != null ? obrigacao.getEmpresa().getId() : null);
        return obrigacao;
    }

    private MensagemResponseDTO toResponseDTO(MensagemObrigacao m, Long idUsuarioLogado) {
        Usuario autor = m.getAutor();
        Arquivo arquivo = m.getArquivo();
        boolean lidaPeloDestinatario = m.isDoEscritorio()
                ? m.getLidaPeloClienteEm() != null
                : m.getLidaPeloEscritorioEm() != null;
        return new MensagemResponseDTO(
                m.getId(),
                m.getObrigacaoPendente().getId(),
                m.getTexto(),
                m.getDataCriacao(),
                autor != null ? autor.getId() : null,
                autor != null ? autor.getNome() : null,
                perfil(m),
                m.isDoEscritorio(),
                autor != null && Objects.equals(autor.getId(), idUsuarioLogado),
                arquivo != null ? arquivo.getId() : null,
                arquivo != null ? arquivo.getNomeOriginal() : null,
                m.getLidaPeloEscritorioEm(),
                m.getLidaPeloClienteEm(),
                lidaPeloDestinatario
        );
    }

    private ConversaRecenteDTO toConversaDTO(MensagemObrigacao m, Long idUsuarioLogado, Map<Long, Long> naoLidas) {
        ObrigacaoPendente o = m.getObrigacaoPendente();
        Usuario autor = m.getAutor();
        String texto = m.getTexto() != null ? m.getTexto().replaceAll("\\s+", " ").trim() : "";
        String previa = texto.length() > TAMANHO_PREVIA ? texto.substring(0, TAMANHO_PREVIA - 1) + "…" : texto;
        return new ConversaRecenteDTO(
                o.getId(),
                o.getObrigacaoRecorrente() != null ? o.getObrigacaoRecorrente().getNome() : "Obrigação",
                competencia(o),
                o.getEmpresa() != null ? o.getEmpresa().getId() : null,
                o.getEmpresa() != null ? o.getEmpresa().getNomeFantasia() : null,
                m.getId(),
                autor != null ? autor.getNome() : null,
                perfil(m),
                m.isDoEscritorio(),
                autor != null && Objects.equals(autor.getId(), idUsuarioLogado),
                previa,
                m.getDataCriacao(),
                naoLidas.getOrDefault(o.getId(), 0L)
        );
    }

    private String perfil(MensagemObrigacao m) {
        return m.isDoEscritorio() ? "ADMIN" : "FUNCIONARIO";
    }

    private String competencia(ObrigacaoPendente p) {
        if (p.getDataVencimento() == null) return null;
        Periodicidade per = p.getObrigacaoRecorrente() != null && p.getObrigacaoRecorrente().getPeriodicidade() != null
                ? p.getObrigacaoRecorrente().getPeriodicidade() : Periodicidade.MENSAL;
        if (per == Periodicidade.ANUAL) {
            return String.valueOf(p.getDataVencimento().getYear() - 1);
        }
        YearMonth ref = YearMonth.from(p.getDataVencimento()).minusMonths(1);
        return String.format("%02d/%d", ref.getMonthValue(), ref.getYear());
    }
}
