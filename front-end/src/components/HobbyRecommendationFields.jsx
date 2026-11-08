export default function HobbyRecommendationFields({
                                                      interesses, objetivos, pesosInteresses, pesosObjetivos, onChange, disabled
                                                  }) {
    function grupo(tipo, titulo, itens, pesos) {
        return (
            <fieldset className="admin-hobby-weight-group" disabled={disabled}>
                <legend>{titulo} — selecione pelo menos um *</legend>
                {itens.length === 0 && <p>Nenhuma opção cadastrada.</p>}
                {itens.map((item) => {
                    const selecionado = Object.hasOwn(pesos, String(item.id));
                    return (
                        <div className="admin-hobby-weight-row" key={item.id}>
                            <label className="admin-hobby-weight-choice">
                                <input
                                    type="checkbox"
                                    checked={selecionado}
                                    onChange={(event) => onChange(
                                        tipo, item.id, event.target.checked ? 1 : null
                                    )}
                                />
                                {item.nome}
                            </label>
                            <label className="admin-hobby-weight-value">
                                Peso de {item.nome}
                                <select
                                    value={selecionado ? pesos[item.id] : ""}
                                    disabled={disabled || !selecionado}
                                    required={selecionado}
                                    onChange={(event) => onChange(
                                        tipo, item.id, Number(event.target.value)
                                    )}
                                >
                                    <option value="" disabled>Selecione</option>
                                    <option value="1">1</option>
                                    <option value="2">2</option>
                                    <option value="3">3</option>
                                </select>
                            </label>
                        </div>
                    );
                })}
            </fieldset>
        );
    }

    return (
        <>
            {grupo("interesses", "Interesses", interesses, pesosInteresses)}
            {grupo("objetivos", "Objetivos", objetivos, pesosObjetivos)}
        </>
    );
}