package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class V31__reabrir_questionarios_inconsistentes extends BaseJavaMigration {

    // Snapshot of the V30 POST contract. Keep the historical migration independent of the current DTO.
    private static final Pattern CIDADE = Pattern.compile("[\\p{L}\\p{M} .’'-]+");
    private static final Pattern UF = Pattern.compile(
            "(?i)AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO");

    @Override
    public void migrate(Context context) throws SQLException {
        var connection = context.getConnection();
        // V1 already enforces the presence and allowed values of the three enum columns.
        try (var leitura = connection.prepareStatement("""
                SELECT id, tempo_disponivel_semanal, orcamento_inicial, cidade, estado
                FROM recommendation_profile
                WHERE questionario_concluido
                """);
             var atualizacao = connection.prepareStatement("""
                UPDATE recommendation_profile SET questionario_concluido = false
                WHERE id = ? AND questionario_concluido
                """)) {
            leitura.setFetchSize(500);
            try (var perfis = leitura.executeQuery()) {
                while (perfis.next()) {
                    if (!valido(perfis)) {
                        atualizacao.setLong(1, perfis.getLong("id"));
                        atualizacao.executeUpdate();
                    }
                }
            }
        }
    }

    private boolean valido(ResultSet perfil) throws SQLException {
        String cidade = perfil.getString("cidade");
        String uf = perfil.getString("estado");
        return perfil.getDouble("tempo_disponivel_semanal") > 0
                && perfil.getDouble("orcamento_inicial") >= 0
                && cidade != null && !cidade.isBlank() && cidade.length() <= 100
                && CIDADE.matcher(cidade).matches()
                && uf != null && UF.matcher(uf).matches();
    }
}
