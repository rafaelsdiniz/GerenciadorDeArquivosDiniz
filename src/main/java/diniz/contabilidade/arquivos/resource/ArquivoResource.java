package diniz.contabilidade.arquivos.resource;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Base64;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.MultipartForm;

import diniz.contabilidade.arquivos.dto.request.ArquivoRequestDTO;
import diniz.contabilidade.arquivos.dto.response.ArquivoResponseDTO;
import diniz.contabilidade.arquivos.model.entity.Arquivo;
import diniz.contabilidade.arquivos.exception.ErroPayload;
import diniz.contabilidade.arquivos.model.enums.CategoriaFiscal;
import diniz.contabilidade.arquivos.model.enums.StatusArquivo;
import diniz.contabilidade.arquivos.resource.form.ArquivoUploadForm;
import diniz.contabilidade.arquivos.service.ArquivoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/arquivos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ArquivoResource {

    private static final Logger LOG = Logger.getLogger(ArquivoResource.class);

    @Inject
    ArquivoService arquivoService;

    @GET
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response listar() {
        return Response.ok(arquivoService.listar()).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response buscarPorId(@PathParam("id") Long id) {
        return Response.ok(arquivoService.buscarPorId(id)).build();
    }

    @GET
    @Path("/empresa/{idEmpresa}")
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response buscarPorEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        return Response.ok(arquivoService.buscarPorEmpresa(idEmpresa)).build();
    }

    @GET
    @Path("/vencendo")
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response vencendoEm(@QueryParam("dias") @DefaultValue("7") int dias) {
        return Response.ok(arquivoService.buscarVencendoEm(dias)).build();
    }

    @GET
    @Path("/vencidos")
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response vencidos() {
        return Response.ok(arquivoService.buscarVencidos()).build();
    }

    @GET
    @Path("/por-status")
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response porStatus(@QueryParam("status") StatusArquivo status) {
        return Response.ok(arquivoService.buscarPorStatus(status)).build();
    }

    @GET
    @Path("/por-categoria")
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response porCategoria(@QueryParam("categoria") CategoriaFiscal categoria) {
        return Response.ok(arquivoService.buscarPorCategoria(categoria)).build();
    }

    @GET
    @Path("/lixeira")
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response lixeira() {
        return Response.ok(arquivoService.listarLixeira()).build();
    }

    @PATCH
    @Path("/{id}/status")
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response atualizarStatus(@PathParam("id") Long id, @QueryParam("status") StatusArquivo status) {
        return Response.ok(arquivoService.atualizarStatus(id, status)).build();
    }

    @PATCH
    @Path("/{id}/vencimento")
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response atualizarVencimento(@PathParam("id") Long id, @QueryParam("dataVencimento") LocalDate dataVencimento) {
        return Response.ok(arquivoService.atualizarVencimento(id, dataVencimento)).build();
    }

    @POST
    @Path("/{id}/restaurar")
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response restaurar(@PathParam("id") Long id) {
        return Response.ok(arquivoService.restaurar(id)).build();
    }

    @PATCH
    @Path("/{id}/pasta")
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response moverParaPasta(@PathParam("id") Long id, @QueryParam("idPasta") Long idPasta) {
        return Response.ok(arquivoService.moverParaPasta(id, idPasta)).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response upload(@MultipartForm @Valid ArquivoUploadForm form) {

        if (form == null) {
            throw new ValidationException("Form inválido.");
        }

        if (form.arquivo == null) {
            throw new ValidationException("Arquivo é obrigatório.");
        }

        try {

            String nomeOriginal = form.arquivo.fileName();
            java.nio.file.Path arquivoTemp = form.arquivo.uploadedFile();

            if (arquivoTemp == null) {
                throw new RuntimeException("Arquivo temporário não encontrado.");
            }

            long tamanho = Files.size(arquivoTemp);

            String extensao = "";
            int i = nomeOriginal.lastIndexOf(".");
            if (i != -1) {
                extensao = nomeOriginal.substring(i);
            }

            String base64String;
            try (InputStream input = Files.newInputStream(arquivoTemp)) {
                byte[] bytes = input.readAllBytes();
                base64String = Base64.getEncoder().encodeToString(bytes);
            }

            ArquivoRequestDTO dto = new ArquivoRequestDTO(
                    form.idEmpresa,
                    form.idUsuario,
                    form.idPasta,
                    form.descricao,
                    form.dataVencimento,
                    form.idObrigacaoPendente,
                    form.categoriaFiscal
            );

            ArquivoResponseDTO response =
                    arquivoService.salvar(
                            dto,
                            nomeOriginal,
                            tamanho,
                            base64String
                    );

            return Response.status(Response.Status.CREATED)
                    .entity(response)
                    .build();

        } catch (IllegalArgumentException | ValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErroPayload("VALIDATION", e.getMessage()))
                    .build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErroPayload("NOT_FOUND", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao enviar arquivo.", e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErroPayload("UPLOAD_ERROR", "Erro ao enviar arquivo: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response download(@PathParam("id") Long id){

        Arquivo arquivo = arquivoService.buscarEntidadePorId(id);

        if (arquivo.getArquivoBase64() == null) {
            throw new NotFoundException("Arquivo físico não encontrado (dado de teste sem conteúdo).");
        }

        byte[] bytes = Base64.getDecoder().decode(arquivo.getArquivoBase64());
        InputStream stream = new ByteArrayInputStream(bytes);

        arquivoService.registrarDownload(id, arquivo.getNomeOriginal());

        return Response.ok(stream)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + arquivo.getNomeOriginal() + "\"")
                .build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response excluir(@PathParam("id") Long id){
        arquivoService.excluir(id);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}/permanente")
    @RolesAllowed("ADMIN")
    public Response excluirPermanente(@PathParam("id") Long id){
        arquivoService.excluirPermanente(id);
        return Response.noContent().build();
    }
}
