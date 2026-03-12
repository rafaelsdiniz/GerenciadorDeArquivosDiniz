package diniz.contabilidade.arquivos.resource.form;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class ArquivoUploadForm {

    @RestForm("arquivo")
    public FileUpload arquivo;

    @RestForm("idEmpresa")
    public Long idEmpresa;

    @RestForm("idUsuario")
    public Long idUsuario;

    @RestForm("idPasta")
    public Long idPasta;
}