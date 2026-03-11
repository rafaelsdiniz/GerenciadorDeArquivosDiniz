package diniz.contabilidade.arquivos.service;

import java.util.List;
import java.util.UUID;

import diniz.contabilidade.arquivos.dto.request.ArquivoRequestDTO;
import diniz.contabilidade.arquivos.dto.response.ArquivoResponseDTO;
import diniz.contabilidade.arquivos.model.entity.Arquivo;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.Pasta;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.model.enums.TipoArquivo;
import diniz.contabilidade.arquivos.repository.ArquivoRepository;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import diniz.contabilidade.arquivos.repository.PastaRepository;
import diniz.contabilidade.arquivos.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ArquivoService {

    @Inject
    ArquivoRepository arquivoRepository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    PastaRepository pastaRepository;

    public List<ArquivoResponseDTO> listar() {
        return arquivoRepository.listAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public ArquivoResponseDTO buscarPorId(Long id) {
        Arquivo arquivo = arquivoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Arquivo não encontrado."));

        return toResponseDTO(arquivo);
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

    @Transactional
    public ArquivoResponseDTO salvar(ArquivoRequestDTO dto, String nomeOriginal, Long tamanho, String caminho) {
        Empresa empresa = empresaRepository.findByIdOptional(dto.idEmpresa())
                .orElseThrow(() -> new NotFoundException("Empresa não encontrada."));

        Usuario usuario = usuarioRepository.findByIdOptional(dto.idUsuario())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        Pasta pasta = pastaRepository.findByIdOptional(dto.idPasta())
                .orElseThrow(() -> new NotFoundException("Pasta não encontrada."));

        if (!usuario.getEmpresa().getId().equals(empresa.getId())) {
            throw new IllegalArgumentException("O usuário não pertence à empresa informada.");
        }

        if (!pasta.getEmpresa().getId().equals(empresa.getId())) {
            throw new IllegalArgumentException("A pasta não pertence à empresa informada.");
        }

        Arquivo arquivo = new Arquivo();
        arquivo.setEmpresa(empresa);
        arquivo.setUsuario(usuario);
        arquivo.setPasta(pasta);
        arquivo.setNomeOriginal(nomeOriginal);
        arquivo.setNome(gerarNomeArquivo(nomeOriginal));
        arquivo.setTamanho(tamanho);
        arquivo.setCaminho(caminho);
        arquivo.setTipoArquivo(identificarTipoArquivo(nomeOriginal));

        arquivoRepository.persist(arquivo);

        return toResponseDTO(arquivo);
    }

    @Transactional
    public void deletar(Long id) {
        Arquivo arquivo = arquivoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Arquivo não encontrado."));

        arquivoRepository.delete(arquivo);
    }

    private ArquivoResponseDTO toResponseDTO(Arquivo arquivo) {
        return new ArquivoResponseDTO(
                arquivo.getId(),
                arquivo.getNome(),
                arquivo.getNomeOriginal(),
                arquivo.getTamanho(),
                arquivo.getTipoArquivo(),
                arquivo.getCaminho(),
                arquivo.getEmpresa().getId(),
                arquivo.getUsuario().getId(),
                arquivo.getPasta().getId()
        );
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