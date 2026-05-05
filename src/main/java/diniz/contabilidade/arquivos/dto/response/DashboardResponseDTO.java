package diniz.contabilidade.arquivos.dto.response;

import java.util.List;
import java.util.Map;

import diniz.contabilidade.arquivos.model.enums.CategoriaFiscal;
import diniz.contabilidade.arquivos.model.enums.StatusArquivo;

public record DashboardResponseDTO(

    Long idEmpresa,
    String nomeEmpresa,
    long totalArquivos,
    long totalArquivosVencidos,
    long totalArquivosVencendo7Dias,
    long totalArquivosVencendo30Dias,
    Double taxaEntregaNoPrazo,
    long totalObrigacoesPendentes,
    long totalObrigacoesVencidas,
    Map<StatusArquivo, Long> distribuicaoPorStatus,
    Map<CategoriaFiscal, Long> distribuicaoPorCategoria,
    List<MaiorAtraso> top5MaioresAtrasos

) {
    public record MaiorAtraso(Long idArquivo, String nome, String descricao, long diasAtraso) {}
}
