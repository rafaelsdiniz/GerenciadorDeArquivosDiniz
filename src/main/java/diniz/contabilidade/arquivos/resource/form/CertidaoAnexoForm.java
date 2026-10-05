package diniz.contabilidade.arquivos.resource.form;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/** Upload do PDF de uma certidão (vai para o Drive da empresa). */
public class CertidaoAnexoForm {

    @RestForm("arquivo")
    public FileUpload arquivo;
}
