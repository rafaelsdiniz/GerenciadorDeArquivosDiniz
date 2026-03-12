package diniz.contabilidade.arquivos.resource;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;

import org.jboss.resteasy.reactive.MultipartForm;

import diniz.contabilidade.arquivos.dto.request.ArquivoRequestDTO;
import diniz.contabilidade.arquivos.dto.response.ArquivoResponseDTO;
import diniz.contabilidade.arquivos.resource.form.ArquivoUploadForm;
import diniz.contabilidade.arquivos.service.ArquivoService;
import diniz.contabilidade.arquivos.service.MinioService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/arquivos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ArquivoResource {

    @Inject
    ArquivoService arquivoService;

    @Inject
    MinioService minioService;

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

    @POST
@Consumes(MediaType.MULTIPART_FORM_DATA)
@RolesAllowed({"ADMIN","FUNCIONARIO"})
public Response upload(@MultipartForm ArquivoUploadForm form) {

    System.out.println("===== INICIO UPLOAD =====");

    if (form == null) {
        System.out.println("Form veio null");
        throw new ValidationException("Form inválido.");
    }

    if (form.arquivo == null) {
        System.out.println("Arquivo veio null");
        throw new ValidationException("Arquivo é obrigatório.");
    }

    try {

        System.out.println("Arquivo recebido");

        String nomeOriginal = form.arquivo.fileName();
        java.nio.file.Path arquivoTemp = form.arquivo.uploadedFile();

        System.out.println("Nome original: " + nomeOriginal);
        System.out.println("Arquivo temp: " + arquivoTemp);
        System.out.println("Content type: " + form.arquivo.contentType());

        if (arquivoTemp == null) {
            System.out.println("Arquivo temporário veio null");
            throw new RuntimeException("Arquivo temporário não encontrado.");
        }

        long tamanho = Files.size(arquivoTemp);
        System.out.println("Tamanho: " + tamanho);

        String extensao = "";
        int i = nomeOriginal.lastIndexOf(".");
        if (i != -1) {
            extensao = nomeOriginal.substring(i);
        }

        System.out.println("Extensão detectada: " + extensao);

        String objectKey =
                "empresa-" + form.idEmpresa +
                "/pasta-" + form.idPasta +
                "/" + UUID.randomUUID() + extensao;

        System.out.println("ObjectKey gerado: " + objectKey);

        System.out.println("Iniciando upload para MinIO...");

        try (InputStream input = Files.newInputStream(arquivoTemp)) {

            minioService.upload(
                    objectKey,
                    input,
                    tamanho,
                    form.arquivo.contentType()
            );

        }

        System.out.println("Upload para MinIO concluído");

        ArquivoRequestDTO dto = new ArquivoRequestDTO(
                form.idEmpresa,
                form.idUsuario,
                form.idPasta
        );

        System.out.println("Salvando metadados no banco...");

        ArquivoResponseDTO response =
                arquivoService.salvar(
                        dto,
                        nomeOriginal,
                        tamanho,
                        objectKey
                );

        System.out.println("Registro salvo no banco com ID: " + response.id());

        System.out.println("===== UPLOAD FINALIZADO =====");

        return Response.status(Response.Status.CREATED)
                .entity(response)
                .build();

    } catch (Exception e) {

        System.out.println("===== ERRO NO UPLOAD =====");
        e.printStackTrace();

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Erro ao enviar arquivo: " + e.getMessage())
                .build();
    }
}

    @GET
    @Path("/{id}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response download(@PathParam("id") Long id){

        ArquivoResponseDTO arquivo = arquivoService.buscarPorId(id);

        InputStream stream = minioService.download(arquivo.caminho());

        return Response.ok(stream)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + arquivo.nomeOriginal() + "\"")
                .build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN","FUNCIONARIO"})
    public Response deletar(@PathParam("id") Long id){

        ArquivoResponseDTO arquivo = arquivoService.buscarPorId(id);

        minioService.delete(arquivo.caminho());

        arquivoService.deletar(id);

        return Response.noContent().build();
    }
}