package diniz.contabilidade.arquivos.service;

import org.jboss.logging.Logger;

import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.repository.UsuarioRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BootstrapSenhasService {

    private static final Logger LOG = Logger.getLogger(BootstrapSenhasService.class);

    @Inject
    UsuarioRepository usuarioRepository;

    // Migra senhas em texto puro vindas do import.sql para BCrypt na primeira subida da app.
    @Transactional
    void onStart(@Observes StartupEvent ev) {
        int migrados = 0;
        for (Usuario u : usuarioRepository.listAll()) {
            String senha = u.getSenha();
            if (senha != null && !senha.startsWith("$2")) {
                u.setSenha(BcryptUtil.bcryptHash(senha));
                migrados++;
            }
        }
        if (migrados > 0) {
            LOG.infof("BCrypt aplicado em %d senha(s) em texto puro do seed.", migrados);
        }
    }
}
