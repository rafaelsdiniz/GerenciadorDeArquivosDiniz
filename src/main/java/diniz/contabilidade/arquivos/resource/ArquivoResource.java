package diniz.contabilidade.arquivos.resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import diniz.contabilidade.arquivos.dto.request.ArquivoRequestDTO;
import diniz.contabilidade.arquivos.dto.response.ArquivoResponseDTO;
import diniz.contabilidade.arquivos.resource.form.ArquivoUploadForm;
import diniz.contabilidade.arquivos.service.ArquivoService;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/arquivos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ArquivoResource {

    @Inject
    ArquivoService arquivoService;

    @ConfigProperty(name = "app.upload.dir", defaultValue = "uploads")
    String uploadDir;

    @GET
    public Response listar() {
        return Response.ok(arquivoService.listar()).build();
    }

    @GET
    @Path("/{id}")
    public Response buscarPorId(@PathParam("id") Long id) {
        return Response.ok(arquivoService.buscarPorId(id)).build();
    }

    @GET
    @Path("/empresa/{idEmpresa}")
    public Response buscarPorEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        return Response.ok(arquivoService.buscarPorEmpresa(idEmpresa)).build();
    }

    @GET
    @Path("/pasta/{idPasta}")
    public Response buscarPorPasta(@PathParam("idPasta") Long idPasta) {
        return Response.ok(arquivoService.buscarPorPasta(idPasta)).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(ArquivoUploadForm form) {
        if (form == null || form.arquivo == null) {
            throw new ValidationException("O arquivo é obrigatório.");
        }

        if (form.idEmpresa == null) {
            throw new ValidationException("O id da empresa é obrigatório.");
        }

        if (form.idUsuario == null) {
            throw new ValidationException("O id do usuário é obrigatório.");
        }

        if (form.idPasta == null) {
            throw new ValidationException("O id da pasta é obrigatório.");
        }

        try {
            java.nio.file.Path diretorio = Paths.get(uploadDir);
            Files.createDirectories(diretorio);

            String nomeOriginal = form.arquivo.fileName();
            java.nio.file.Path arquivoTemporario = form.arquivo.uploadedFile();

            java.nio.file.Path destino = diretorio.resolve(nomeOriginal);
            Files.copy(arquivoTemporario, destino, StandardCopyOption.REPLACE_EXISTING);

            ArquivoRequestDTO dto = new ArquivoRequestDTO(
                    form.idEmpresa,
                    form.idUsuario,
                    form.idPasta
            );

            ArquivoResponseDTO responseDTO = arquivoService.salvar(
                    dto,
                    nomeOriginal,
                    Files.size(destino),
                    destino.toString()
            );

            return Response.status(Response.Status.CREATED)
                    .entity(responseDTO)
                    .build();

        } catch (NotFoundException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (IOException e) {
            throw new WebApplicationException("Erro ao salvar arquivo.", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deletar(@PathParam("id") Long id) {
        arquivoService.deletar(id);
        return Response.noContent().build();
    }
}