package diniz.contabilidade.arquivos.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import diniz.contabilidade.arquivos.dto.request.ArquivoRequestDTO;
import diniz.contabilidade.arquivos.dto.response.ArquivoResponseDTO;
import diniz.contabilidade.arquivos.model.entity.Arquivo;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoPendente;
import diniz.contabilidade.arquivos.model.entity.Pasta;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.model.enums.AcaoLog;
import diniz.contabilidade.arquivos.model.enums.CategoriaFiscal;
import diniz.contabilidade.arquivos.model.enums.PerfilUsuario;
import diniz.contabilidade.arquivos.model.enums.StatusArquivo;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
import diniz.contabilidade.arquivos.model.enums.TipoArquivo;
import diniz.contabilidade.arquivos.repository.ArquivoRepository;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import diniz.contabilidade.arquivos.repository.ObrigacaoPendenteRepository;
import diniz.contabilidade.arquivos.repository.PastaRepository;
import diniz.contabilidade.arquivos.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ArquivoService {

    private static final String ENTIDADE = "Arquivo";

    @Inject
    ArquivoRepository arquivoRepository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    PastaRepository pastaRepository;

    @Inject
    ObrigacaoPendenteRepository obrigacaoPendenteRepository;

    @Inject
    LogAcessoService logService;

    public List<ArquivoResponseDTO> listar() {
        return arquivoRepository.listarAtivos()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<ArquivoResponseDTO> listarLixeira() {
        return arquivoRepository.listarLixeira()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public ArquivoResponseDTO buscarPorId(Long id) {
        Arquivo arquivo = arquivoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Arquivo não encontrado."));

        return toResponseDTO(arquivo);
    }

    public Arquivo buscarEntidadePorId(Long id) {
        return arquivoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Arquivo não encontrado."));
    }

    public List<ArquivoResponseDTO> buscarPorEmpresa(Long idEmpresa) {
        Empresa empresa = empresaRepository.findByIdOptional(idEmpresa)
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        return arquivoRepository.buscarPorEmpresa(empresa)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<ArquivoResponseDTO> buscarPorPasta(Long idPasta) {
        Pasta pasta = pastaRepository.findByIdOptional(idPasta)
                .orElseThrow(() -> new NotFoundException("Pasta não encontrada."));

        return arquivoRepository.buscarPorPasta(pasta)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<ArquivoResponseDTO> buscarVencendoEm(int dias) {
        return arquivoRepository.buscarVencendoEm(dias)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<ArquivoResponseDTO> buscarVencidos() {
        return arquivoRepository.buscarVencidos()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<ArquivoResponseDTO> buscarPorStatus(StatusArquivo status) {
        return arquivoRepository.buscarPorStatus(status)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<ArquivoResponseDTO> buscarPorCategoria(CategoriaFiscal categoria) {
        return arquivoRepository.buscarPorCategoria(categoria)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public ArquivoResponseDTO salvar(ArquivoRequestDTO dto, String nomeOriginal, Long tamanho, String arquivoBase64) {
        Empresa empresa = empresaRepository.findByIdOptional(dto.idEmpresa())
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        Usuario usuario = usuarioRepository.findByIdOptional(dto.idUsuario())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        Pasta pasta = pastaRepository.findByIdOptional(dto.idPasta())
                .orElseThrow(() -> new NotFoundException("Pasta não encontrada."));

        boolean ehAdmin = usuario.getPerfilUsuario() == PerfilUsuario.ADMIN;

        if (!ehAdmin && !usuario.getEmpresa().getId().equals(empresa.getId())) {
            throw new IllegalArgumentException("Funcionário só pode enviar arquivos para a sua própria empresa.");
        }

        if (!pasta.getEmpresa().getId().equals(empresa.getId())) {
            throw new IllegalArgumentException("A pasta selecionada não pertence à empresa informada.");
        }

        Arquivo arquivo = new Arquivo();
        arquivo.setEmpresa(empresa);
        arquivo.setUsuario(usuario);
        arquivo.setPasta(pasta);
        arquivo.setNomeOriginal(nomeOriginal);
        arquivo.setNome(gerarNomeArquivo(nomeOriginal));
        arquivo.setTamanho(tamanho);
        arquivo.setCaminho(null);
        arquivo.setArquivoBase64(arquivoBase64);
        arquivo.setTipoArquivo(identificarTipoArquivo(nomeOriginal));
        arquivo.setCategoriaFiscal(dto.categoriaFiscal());
        arquivo.setDescricao(dto.descricao());
        arquivo.setDataVencimento(dto.dataVencimento());

        boolean vinculadoAObrigacao = false;
        if (dto.idObrigacaoPendente() != null) {
            ObrigacaoPendente obrigacao = obrigacaoPendenteRepository.findByIdOptional(dto.idObrigacaoPendente())
                    .orElseThrow(() -> new NotFoundException("Obrigação pendente não encontrada."));

            if (!obrigacao.getEmpresa().getId().equals(empresa.getId())) {
                throw new IllegalArgumentException("A obrigação não pertence à empresa informada.");
            }

            arquivo.setObrigacaoPendente(obrigacao);
            obrigacao.setStatus(StatusObrigacao.ENTREGUE);
            obrigacao.setDataEntrega(LocalDate.now());

            if (arquivo.getDataVencimento() == null) {
                arquivo.setDataVencimento(obrigacao.getDataVencimento());
            }
            vinculadoAObrigacao = true;
        }

        arquivo.setStatus(definirStatusInicial(vinculadoAObrigacao, arquivo.getDataVencimento()));

        arquivoRepository.persist(arquivo);

        logService.registrar(AcaoLog.UPLOAD, ENTIDADE, arquivo.getId(),
                "Upload de \"" + nomeOriginal + "\" (empresa " + empresa.getId() + ")");

        return toResponseDTO(arquivo);
    }

    @Transactional
    public ArquivoResponseDTO atualizarStatus(Long id, StatusArquivo status) {
        Arquivo arquivo = buscarAtivo(id);
        StatusArquivo anterior = arquivo.getStatus();
        arquivo.setStatus(status);
        logService.registrar(AcaoLog.ATUALIZAR_STATUS, ENTIDADE, id,
                "Status: " + anterior + " -> " + status);
        return toResponseDTO(arquivo);
    }

    @Transactional
    public ArquivoResponseDTO atualizarVencimento(Long id, LocalDate dataVencimento) {
        Arquivo arquivo = buscarAtivo(id);
        LocalDate anterior = arquivo.getDataVencimento();
        arquivo.setDataVencimento(dataVencimento);
        logService.registrar(AcaoLog.ATUALIZAR_VENCIMENTO, ENTIDADE, id,
                "Vencimento: " + anterior + " -> " + dataVencimento);
        return toResponseDTO(arquivo);
    }

    @Transactional
    public void excluir(Long id) {
        Arquivo arquivo = buscarAtivo(id);
        arquivo.setExcluidoEm(LocalDateTime.now());
        logService.registrar(AcaoLog.EXCLUIR, ENTIDADE, id,
                "Movido para a lixeira: " + arquivo.getNomeOriginal());
    }

    @Transactional
    public ArquivoResponseDTO moverParaPasta(Long id, Long idPasta) {
        Arquivo arquivo = buscarAtivo(id);
        Pasta destino = pastaRepository.findByIdOptional(idPasta)
                .orElseThrow(() -> new NotFoundException("Pasta não encontrada."));

        if (!destino.getEmpresa().getId().equals(arquivo.getEmpresa().getId())) {
            throw new IllegalArgumentException("A pasta de destino não pertence à empresa do arquivo.");
        }

        Long idOrigem = arquivo.getPasta() != null ? arquivo.getPasta().getId() : null;
        arquivo.setPasta(destino);
        logService.registrar(AcaoLog.ATUALIZAR_STATUS, ENTIDADE, id,
                "Pasta: " + idOrigem + " -> " + idPasta);
        return toResponseDTO(arquivo);
    }

    @Transactional
    public ArquivoResponseDTO restaurar(Long id) {
        Arquivo arquivo = arquivoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Arquivo não encontrado."));
        if (arquivo.getExcluidoEm() == null) {
            throw new IllegalArgumentException("Arquivo não está na lixeira.");
        }
        arquivo.setExcluidoEm(null);
        logService.registrar(AcaoLog.RESTAURAR, ENTIDADE, id,
                "Restaurado da lixeira: " + arquivo.getNomeOriginal());
        return toResponseDTO(arquivo);
    }

    @Transactional
    public void excluirPermanente(Long id) {
        Arquivo arquivo = arquivoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Arquivo não encontrado."));

        logService.registrar(AcaoLog.EXCLUIR_PERMANENTE, ENTIDADE, id,
                "Excluído permanentemente: " + arquivo.getNomeOriginal());
        arquivoRepository.delete(arquivo);
    }

    public void registrarDownload(Long id, String nomeOriginal) {
        logService.registrar(AcaoLog.DOWNLOAD, ENTIDADE, id, "Download: " + nomeOriginal);
    }

    private Arquivo buscarAtivo(Long id) {
        Arquivo arquivo = arquivoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Arquivo não encontrado."));
        if (arquivo.getExcluidoEm() != null) {
            throw new IllegalArgumentException("Arquivo está na lixeira; restaure antes de alterá-lo.");
        }
        return arquivo;
    }

    private ArquivoResponseDTO toResponseDTO(Arquivo arquivo) {
        Long diasParaVencer = null;
        if (arquivo.getDataVencimento() != null) {
            diasParaVencer = ChronoUnit.DAYS.between(LocalDate.now(), arquivo.getDataVencimento());
        }

        return new ArquivoResponseDTO(
                arquivo.getId(),
                arquivo.getNome(),
                arquivo.getNomeOriginal(),
                arquivo.getTamanho(),
                arquivo.getTipoArquivo(),
                arquivo.getCategoriaFiscal(),
                arquivo.getCaminho(),
                arquivo.getDescricao(),
                arquivo.getDataVencimento(),
                arquivo.getStatus(),
                diasParaVencer,
                arquivo.getExcluidoEm(),
                arquivo.getEmpresa().getId(),
                arquivo.getUsuario().getId(),
                arquivo.getPasta().getId(),
                arquivo.getObrigacaoPendente() != null ? arquivo.getObrigacaoPendente().getId() : null
        );
    }

    private StatusArquivo definirStatusInicial(boolean vinculadoAObrigacao, LocalDate dataVencimento) {
        if (vinculadoAObrigacao) {
            return StatusArquivo.ENTREGUE;
        }
        if (dataVencimento == null) {
            return StatusArquivo.ARQUIVADO;
        }
        if (dataVencimento.isBefore(LocalDate.now())) {
            return StatusArquivo.VENCIDO;
        }
        return StatusArquivo.PENDENTE;
    }

    private String gerarNomeArquivo(String nomeOriginal) {
        String extensao = "";
        int indice = nomeOriginal.lastIndexOf(".");

        if (indice != -1) {
            extensao = nomeOriginal.substring(indice);
        }

        return UUID.randomUUID() + extensao;
    }

    private TipoArquivo identificarTipoArquivo(String nomeOriginal) {
        String nome = nomeOriginal.toLowerCase();

        if (nome.endsWith(".pdf")) return TipoArquivo.PDF;
        if (nome.endsWith(".xml")) return TipoArquivo.XML;
        if (nome.endsWith(".xls")) return TipoArquivo.XLS;
        if (nome.endsWith(".xlsx")) return TipoArquivo.XLSX;
        if (nome.endsWith(".csv")) return TipoArquivo.CSV;
        if (nome.endsWith(".doc")) return TipoArquivo.DOC;
        if (nome.endsWith(".docx")) return TipoArquivo.DOCX;
        if (nome.endsWith(".txt")) return TipoArquivo.TXT;
        if (nome.endsWith(".jpg")) return TipoArquivo.JPG;
        if (nome.endsWith(".png")) return TipoArquivo.PNG;

        return TipoArquivo.OUTRO;
    }
}
