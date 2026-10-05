package diniz.contabilidade.arquivos.resource;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import org.jboss.logging.Logger;

import diniz.contabilidade.arquivos.dto.request.CertidaoRequestDTO;
import diniz.contabilidade.arquivos.dto.response.CertidaoResponseDTO;
import diniz.contabilidade.arquivos.exception.ErroPayload;
import diniz.contabilidade.arquivos.resource.form.CertidaoAnexoForm;
import diniz.contabilidade.arquivos.security.UsuarioLogado;
import diniz.contabilidade.arquivos.service.CertidaoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Certidões negativas de débitos (CND) com controle de validade.
 * ADMIN (escritório) cadastra e altera; FUNCIONARIO (cliente) só consulta as da própria empresa.
 */
@Path("/certidoes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CertidaoResource {

    private static final Logger LOG = Logger.getLogger(CertidaoResource.class);
    private static final List<String> EXTENSOES = List.of(".pdf", ".png", ".jpg", ".jpeg");
    private static final long TAMANHO_MAXIMO = 10L * 1024 * 1024;

    @Inject
    CertidaoService service;

    @Inject
    UsuarioLogado usuario;

    private CertidaoResponseDTO visivel(Long id) {
        return usuario.verificar(service.buscarPorId(id), CertidaoResponseDTO::idEmpresa);
    }

    /** Certidões visíveis ao usuário; ?empresa= filtra uma empresa. */
    @GET
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public List<CertidaoResponseDTO> listar(@QueryParam("empresa") Long idEmpresa) {
        if (idEmpresa != null) {
            usuario.exigirEmpresa(idEmpresa);
            return service.listarPorEmpresa(idEmpresa);
        }
        return usuario.filtrar(service.listar(), CertidaoResponseDTO::idEmpresa);
    }

    /** Contagem por situação de validade (válidas, vencendo em 15 dias, vencidas). */
    @GET
    @Path("/resumo")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public Response resumo(@QueryParam("empresa") Long idEmpresa) {
        return Response.ok(service.resumo(listar(idEmpresa))).build();
    }

    @GET
    @Path("/empresa/{idEmpresa}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public List<CertidaoResponseDTO> porEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        usuario.exigirEmpresa(idEmpresa);
        return service.listarPorEmpresa(idEmpresa);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "FUNCIONARIO"})
    public CertidaoResponseDTO buscarPorId(@PathParam("id") Long id) {
        return visivel(id);
    }

    @POST
    @RolesAllowed("ADMIN")
    public Response criar(@Valid CertidaoRequestDTO dto) {
        return Response.status(Response.Status.CREATED).entity(service.criar(dto)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public CertidaoResponseDTO atualizar(@PathParam("id") Long id, @Valid CertidaoRequestDTO dto) {
        return service.atualizar(id, dto);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response excluir(@PathParam("id") Long id) {
        service.excluir(id);
        return Response.noContent().build();
    }

    /** Envia o PDF da certidão para o Drive da empresa e vincula à certidão. */
    @POST
    @Path("/{id}/anexo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed("ADMIN")
    public Response anexar(@PathParam("id") Long id, CertidaoAnexoForm form) {
        if (form == null || form.arquivo == null || form.arquivo.uploadedFile() == null) {
            return erro(Response.Status.BAD_REQUEST, "Selecione o arquivo da certidão.");
        }
        String nome = form.arquivo.fileName() != null ? form.arquivo.fileName() : "certidao.pdf";
        String minusculo = nome.toLowerCase(Locale.ROOT);
        if (EXTENSOES.stream().noneMatch(minusculo::endsWith)) {
            return erro(Response.Status.BAD_REQUEST, "Envie a certidão em PDF (ou imagem PNG/JPG).");
        }
        try {
            java.nio.file.Path tmp = form.arquivo.uploadedFile();
            long tamanho = Files.size(tmp);
            if (tamanho == 0) return erro(Response.Status.BAD_REQUEST, "O arquivo está vazio.");
            if (tamanho > TAMANHO_MAXIMO) return erro(Response.Status.BAD_REQUEST, "O arquivo passa de 10 MB.");
            String base64;
            try (InputStream in = Files.newInputStream(tmp)) {
                base64 = Base64.getEncoder().encodeToString(in.readAllBytes());
            }
            Long idUsuario = usuario.usuarioId();
            return Response.ok(service.anexar(id, idUsuario, nome, tamanho, base64)).build();
        } catch (java.io.IOException e) {
            LOG.error("Erro ao ler o arquivo da certidão.", e);
            return erro(Response.Status.INTERNAL_SERVER_ERROR, "Não foi possível ler o arquivo enviado.");
        }
    }

    private static Response erro(Response.Status status, String mensagem) {
        return Response.status(status).entity(new ErroPayload("VALIDATION", mensagem)).type(MediaType.APPLICATION_JSON).build();
    }
}
