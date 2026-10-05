package diniz.contabilidade.arquivos.support;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import diniz.contabilidade.arquivos.model.entity.Arquivo;
import diniz.contabilidade.arquivos.model.entity.Empresa;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoPendente;
import diniz.contabilidade.arquivos.model.entity.ObrigacaoRecorrente;
import diniz.contabilidade.arquivos.model.entity.Pasta;
import diniz.contabilidade.arquivos.model.entity.Socio;
import diniz.contabilidade.arquivos.model.entity.Usuario;
import diniz.contabilidade.arquivos.model.enums.PerfilUsuario;
import diniz.contabilidade.arquivos.model.enums.Periodicidade;
import diniz.contabilidade.arquivos.model.enums.ResponsavelObrigacao;
import diniz.contabilidade.arquivos.model.enums.StatusArquivo;
import diniz.contabilidade.arquivos.model.enums.StatusObrigacao;
import diniz.contabilidade.arquivos.model.enums.TipoArquivo;
import diniz.contabilidade.arquivos.model.valueObject.Cnpj;
import diniz.contabilidade.arquivos.model.valueObject.Cpf;
import diniz.contabilidade.arquivos.model.valueObject.Email;
import diniz.contabilidade.arquivos.model.valueObject.Telefone;
import diniz.contabilidade.arquivos.repository.ArquivoRepository;
import diniz.contabilidade.arquivos.repository.EmpresaRepository;
import diniz.contabilidade.arquivos.repository.ObrigacaoPendenteRepository;
import diniz.contabilidade.arquivos.repository.ObrigacaoRecorrenteRepository;
import diniz.contabilidade.arquivos.repository.PastaRepository;
import diniz.contabilidade.arquivos.repository.SocioRepository;
import diniz.contabilidade.arquivos.repository.UsuarioRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Monta os dados de cada cenário de teste direto pelos repositórios.
 *
 * Cada chamada gera CNPJ, CPF e e-mail únicos, então os testes não dependem
 * da ordem de execução nem dos dados criados por outros testes.
 */
@ApplicationScoped
public class DadosTeste {

    /** Senha de todos os usuários criados pelos testes. */
    public static final String SENHA_PADRAO = "senha-teste-123";

    private static final AtomicLong SEQUENCIA = new AtomicLong(System.currentTimeMillis() % 1_000_000);

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    ArquivoRepository arquivoRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    PastaRepository pastaRepository;

    @Inject
    SocioRepository socioRepository;

    @Inject
    ObrigacaoRecorrenteRepository recorrenteRepository;

    @Inject
    ObrigacaoPendenteRepository pendenteRepository;

    /** CNPJ único (só dígitos) para cadastros de teste. */
    public static String novoCnpj() {
        return String.format("4%013d", SEQUENCIA.incrementAndGet());
    }

    private static long proximo() {
        return SEQUENCIA.incrementAndGet();
    }

    @Transactional
    public Empresa empresa(String nome) {
        return empresa(nome, novoCnpj());
    }

    @Transactional
    public Empresa empresa(String nome, String cnpj) {
        Empresa e = new Empresa();
        e.setNomeFantasia(nome);
        e.setRazaoSocial(nome + " LTDA");
        e.setCnpj(new Cnpj(cnpj));
        e.setTelefone(new Telefone("63999990000"));
        e.setEmail(new Email("empresa" + proximo() + "@teste.com"));
        empresaRepository.persist(e);
        return e;
    }

    @Transactional
    public Usuario admin(Empresa escritorio) {
        return usuario(escritorio, PerfilUsuario.ADMIN);
    }

    @Transactional
    public Usuario funcionario(Empresa empresa) {
        return usuario(empresa, PerfilUsuario.FUNCIONARIO);
    }

    @Transactional
    public Usuario usuario(Empresa empresa, PerfilUsuario perfil) {
        Usuario u = new Usuario();
        u.setNome("Usuário " + perfil.name().toLowerCase());
        u.setEmail(new Email("usuario" + proximo() + "@teste.com"));
        u.setSenha(BcryptUtil.bcryptHash(SENHA_PADRAO));
        u.setPerfilUsuario(perfil);
        u.setEmpresa(empresa);
        usuarioRepository.persist(u);
        return u;
    }

    @Transactional
    public Pasta pasta(Empresa empresa) {
        Pasta p = new Pasta();
        p.setNome("Fiscal");
        p.setDescricao("Pasta de teste");
        p.setEmpresa(empresa);
        pastaRepository.persist(p);
        return p;
    }

    @Transactional
    public Socio socio(Empresa empresa) {
        Socio s = new Socio();
        s.setNome("Sócio de teste");
        s.setCpf(new Cpf(String.format("7%010d", proximo())));
        s.setEmpresa(empresa);
        s.setParticipacao(50.0);
        s.setAdministrador(true);
        socioRepository.persist(s);
        return s;
    }

    @Transactional
    public ObrigacaoRecorrente recorrente(Empresa empresa, Periodicidade periodicidade, int diaVencimento,
                                          ResponsavelObrigacao responsavel) {
        ObrigacaoRecorrente r = new ObrigacaoRecorrente();
        r.setEmpresa(empresa);
        r.setNome(responsavel == ResponsavelObrigacao.CLIENTE ? "Extratos bancários" : "DAS - Simples Nacional");
        r.setPeriodicidade(periodicidade);
        r.setDiaVencimento(diaVencimento);
        // inativa: só os testes decidem quando gerar ocorrências
        r.setAtivo(false);
        r.setResponsavel(responsavel);
        recorrenteRepository.persist(r);
        return r;
    }

    @Transactional
    public ObrigacaoPendente pendente(ObrigacaoRecorrente recorrente, LocalDate vencimento, StatusObrigacao status) {
        ObrigacaoPendente p = new ObrigacaoPendente();
        p.setObrigacaoRecorrente(recorrente);
        p.setEmpresa(recorrente.getEmpresa());
        p.setDataVencimento(vencimento);
        p.setStatus(status);
        if (status == StatusObrigacao.ENTREGUE) {
            p.setDataEntrega(LocalDate.now().minusDays(1));
        }
        pendenteRepository.persist(p);
        return p;
    }

    /** Pendência de uma obrigação do escritório (DAS mensal, dia 20). */
    @Transactional
    public ObrigacaoPendente pendenteDoEscritorio(Empresa empresa, LocalDate vencimento, StatusObrigacao status) {
        return pendente(recorrente(empresa, Periodicidade.MENSAL, 20, ResponsavelObrigacao.ESCRITORIO), vencimento, status);
    }

    /** Arquivo já armazenado (sem passar pelo upload). */
    @Transactional
    public Arquivo arquivo(Empresa empresa, Usuario autor, Pasta pasta) {
        Arquivo a = new Arquivo();
        a.setEmpresa(empresa);
        a.setUsuario(autor);
        a.setPasta(pasta);
        a.setNomeOriginal("contrato-social.pdf");
        a.setNome(proximo() + ".pdf");
        a.setTamanho(5L);
        a.setTipoArquivo(TipoArquivo.PDF);
        a.setStatus(StatusArquivo.ARQUIVADO);
        a.setArquivoBase64("JVBERi0=");
        arquivoRepository.persist(a);
        return a;
    }

    @Transactional
    public long contarPendentes(ObrigacaoRecorrente recorrente) {
        return pendenteRepository.count("obrigacaoRecorrente.id", recorrente.getId());
    }
}
