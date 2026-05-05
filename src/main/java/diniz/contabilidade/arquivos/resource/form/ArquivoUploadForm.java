package diniz.contabilidade.arquivos.resource.form;

import java.time.LocalDate;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import diniz.contabilidade.arquivos.model.enums.CategoriaFiscal;
import jakarta.validation.constraints.NotNull;

public class ArquivoUploadForm {

    @RestForm("arquivo")
    @NotNull(message = "O arquivo é obrigatório.")
    public FileUpload arquivo;

    @RestForm("idEmpresa")
    @NotNull(message = "O id da empresa é obrigatório.")
    public Long idEmpresa;

    @RestForm("idUsuario")
    @NotNull(message = "O id do usuário é obrigatório.")
    public Long idUsuario;

    @RestForm("idPasta")
    @NotNull(message = "O id da pasta é obrigatório.")
    public Long idPasta;

    @RestForm("descricao")
    public String descricao;

    @RestForm("dataVencimento")
    public LocalDate dataVencimento;

    @RestForm("idObrigacaoPendente")
    public Long idObrigacaoPendente;

    @RestForm("categoriaFiscal")
    public CategoriaFiscal categoriaFiscal;
}
