import { session } from "../auth/session.js";

import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { api } from "../api/api";
import "./css/SuggestHobby.css";

const estadoInicial = {
    nome: "",
    descricao: "",
    custoEstimado: "",
    custoRecorrenteEstimado: "",
    nivelDificuldade: "1",
    tempoNecessario: "",
    tipoSocializacao: "INDIVIDUAL",
    nivelAtividadeFisica: "BAIXO",
    ambiente: "CASA",
    requerEquipamento: false,
    categoryId: ""
};

export default function SuggestHobby() {
    const navigate = useNavigate();

    const [form, setForm] = useState(estadoInicial);
    const [enviando, setEnviando] = useState(false);
    const [erro, setErro] = useState("");
    const [sucesso, setSucesso] = useState("");
    const [categorias, setCategorias] = useState([]);
    const [carregandoCategorias, setCarregandoCategorias] = useState(true);
    const [erroCategorias, setErroCategorias] = useState("");

    useEffect(() => {
        const controller = new AbortController();

        async function carregarCategorias() {
            try {
                const response = await api.get(
                    "/hobby-categories",
                    {
                        signal: controller.signal,
                        headers: {
                            Authorization:
                                `Bearer ${session.getItem("token")}`
                        }
                    }
                );

                setCategorias(response.data);

            } catch (error) {
                if (
                    error.code === "ERR_CANCELED" ||
                    error.name === "CanceledError"
                ) {
                    return;
                }


                setErroCategorias(
                    "Não foi possível carregar as categorias."
                );

            } finally {
                if (!controller.signal.aborted) {
                    setCarregandoCategorias(false);
                }
            }
        }

        carregarCategorias();

        return () => controller.abort();
    }, []);

    function alterar(event) {
        const { name, value, type, checked } = event.target;

        setForm((anterior) => ({
            ...anterior,
            [name]: type === "checkbox" ? checked : value
        }));
    }

    async function enviar(event) {
        event.preventDefault();

        setEnviando(true);
        setErro("");
        setSucesso("");

        try {
            const payload = {
                ...form,
                custoEstimado: form.custoEstimado === ""
                    ? null : Number(form.custoEstimado),
                custoRecorrenteEstimado:
                    form.custoRecorrenteEstimado === ""
                        ? null : Number(form.custoRecorrenteEstimado),
                nivelDificuldade: Number(form.nivelDificuldade),
                tempoNecessario: Number(form.tempoNecessario),
                categoryId: Number(form.categoryId)
            };

            await api.post("/hobby-suggestions", payload, {
                headers: {
                    Authorization:
                        `Bearer ${session.getItem("token")}`
                }
            });

            setSucesso(
                "Sugestão enviada com sucesso! " +
                "Ela será analisada pela administração."
            );

            setForm(estadoInicial);

        } catch (error) {


            setErro(
                error.response?.data?.message ||
                "Não foi possível enviar a sugestão."
            );

        } finally {
            setEnviando(false);
        }
    }

    return (
        <main className="suggest-hobby-page">
            <div className="suggest-hobby-container">
                <button
                    type="button"
                    className="suggest-back-button"
                    onClick={() => navigate("/community")}
                >
                    ← Voltar à comunidade
                </button>

                <div className="suggest-hobby-header">
                    <h1>Sugerir novo hobby</h1>
                    <p>
                        Compartilhe uma atividade que você gostaria
                        de ver no catálogo do Recommendi.a.
                    </p>
                </div>

                <form
                    className="suggest-hobby-form"
                    onSubmit={enviar}
                >
                    <div className="suggest-field">
                        <label htmlFor="nome">Nome do hobby</label>
                        <input
                            id="nome"
                            name="nome"
                            value={form.nome}
                            onChange={alterar}
                            maxLength={150}
                            required
                        />
                    </div>

                    <div className="suggest-field">
                        <label htmlFor="descricao">Descrição</label>
                        <textarea
                            id="descricao"
                            name="descricao"
                            value={form.descricao}
                            onChange={alterar}
                            rows={4}
                            required
                        />
                    </div>

                    <div className="suggest-field">
                        <label htmlFor="categoryId">
                            Categoria do hobby
                        </label>

                        <select
                            id="categoryId"
                            name="categoryId"
                            value={form.categoryId}
                            onChange={alterar}
                            required
                            disabled={
                                carregandoCategorias ||
                                categorias.length === 0
                            }
                        >
                            <option value="">
                                {carregandoCategorias
                                    ? "Carregando categorias..."
                                    : "Selecione uma categoria"}
                            </option>

                            {categorias.map((categoria) => (
                                <option
                                    key={categoria.id}
                                    value={categoria.id}
                                >
                                    {categoria.nome}
                                </option>
                            ))}
                        </select>

                        {erroCategorias && (
                            <small className="suggest-category-error">
                                {erroCategorias}
                            </small>
                        )}
                    </div>

                    <div className="suggest-form-grid">
                        <div className="suggest-field">
                            <label htmlFor="custoEstimado">
                                Custo inicial (R$)
                            </label>
                            <input
                                id="custoEstimado"
                                name="custoEstimado"
                                type="number"
                                min="0"
                                step="0.01"
                                value={form.custoEstimado}
                                onChange={alterar}
                            />
                        </div>

                        <div className="suggest-field">
                            <label htmlFor="custoRecorrenteEstimado">
                                Custo recorrente (R$)
                            </label>
                            <input
                                id="custoRecorrenteEstimado"
                                name="custoRecorrenteEstimado"
                                type="number"
                                min="0"
                                step="0.01"
                                value={form.custoRecorrenteEstimado}
                                onChange={alterar}
                            />
                        </div>

                        <div className="suggest-field">
                            <label htmlFor="nivelDificuldade">
                                Dificuldade
                            </label>
                            <select
                                id="nivelDificuldade"
                                name="nivelDificuldade"
                                value={form.nivelDificuldade}
                                onChange={alterar}
                            >
                                {[1, 2, 3, 4, 5].map((nivel) => (
                                    <option key={nivel} value={nivel}>
                                        {nivel}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className="suggest-field">
                            <label htmlFor="tempoNecessario">
                                Tempo necessário (horas)
                            </label>
                            <input
                                id="tempoNecessario"
                                name="tempoNecessario"
                                type="number"
                                min="0.1"
                                step="0.1"
                                value={form.tempoNecessario}
                                onChange={alterar}
                                required
                            />
                        </div>

                        <div className="suggest-field">
                            <label htmlFor="tipoSocializacao">Socialização</label>
                            <select
                                id="tipoSocializacao"
                                name="tipoSocializacao"
                                value={form.tipoSocializacao}
                                onChange={alterar}
                            >
                                <option value="INDIVIDUAL">Individual</option>
                                <option value="SOCIAL">Social</option>
                                <option value="INDIFERENTE">Indiferente</option>
                            </select>
                        </div>

                        <div className="suggest-field">
                            <label htmlFor="nivelAtividadeFisica">Atividade física</label>
                            <select
                                id="nivelAtividadeFisica"
                                name="nivelAtividadeFisica"
                                value={form.nivelAtividadeFisica}
                                onChange={alterar}
                            >
                                <option value="BAIXO">Baixa</option>
                                <option value="MODERADO">Moderada</option>
                                <option value="ALTO">Alta</option>
                                <option value="INDIFERENTE">Indiferente</option>
                            </select>
                        </div>

                        <div className="suggest-field">
                            <label htmlFor="ambiente">Ambiente</label>
                            <select
                                id="ambiente"
                                name="ambiente"
                                value={form.ambiente}
                                onChange={alterar}
                            >
                                <option value="CASA">Em casa</option>
                                <option value="AO_AR_LIVRE">Ao ar livre</option>
                                <option value="AMBIENTE_FECHADO">Ambiente fechado</option>
                                <option value="INDIFERENTE">Indiferente</option>
                            </select>
                        </div>

                    </div>

                    <label className="suggest-checkbox">
                        <input
                            name="requerEquipamento"
                            type="checkbox"
                            checked={form.requerEquipamento}
                            onChange={alterar}
                        />
                        Requer equipamento específico
                    </label>

                    {erro && (
                        <p className="suggest-error" role="alert">
                            {erro}
                        </p>
                    )}

                    {sucesso && (
                        <p className="suggest-success" role="status">
                            {sucesso}
                        </p>
                    )}

                    <button
                        className="suggest-submit-button"
                        type="submit"
                        disabled={enviando}
                    >
                        {enviando
                            ? "Enviando..."
                            : "Enviar sugestão"}
                    </button>
                </form>
            </div>
        </main>
    );
}
